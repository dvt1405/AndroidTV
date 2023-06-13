package com.kt.apps.core.extensions

import android.util.Log
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.di.NetworkModule
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.utils.DATE_TIME_FORMAT
import com.kt.apps.core.utils.DATE_TIME_FORMAT_0700
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.toDate
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.simpleframework.xml.stream.InputNode
import org.simpleframework.xml.stream.NodeBuilder
import java.lang.Exception
import java.util.Calendar
import java.util.Locale
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Named

@CoreScope
class ParserExtensionsProgramSchedule @Inject constructor(
    private val client: OkHttpClient,
    private val storage: IKeyValueStorage,
    private val roomDataBase: RoomDataBase,
    @Named(NetworkModule.EXTRA_NETWORK_DISPOSABLE)
    private val disposable: DisposableContainer
) {
    private val pool by lazy {
        Schedulers.io()

    }
    private val extensionsProgramDao by lazy {
        roomDataBase.extensionsTVChannelProgramDao()
    }

    fun getListProgramForExtensionsChannel(
        channel: ExtensionsChannel
    ): Observable<List<TVScheduler.Programme>> {
        return getListProgramForChannel(channel.channelId, true)
    }

    fun getListProgramForTVChannel(
        tvChannel: TVChannelDTO
    ): Observable<List<TVScheduler.Programme>> {
        return getListProgramForChannel(tvChannel.channelId, false)
    }

    private fun getListProgramForChannel(
        channelId: String,
        useAbsoluteId: Boolean
    ): Observable<List<TVScheduler.Programme>> {
        val queryChannelId = if (useAbsoluteId) {
            channelId
        } else {
            channelId
                .removeAllSpecialChars()
                .removePrefix("viechannel")
        }
        return if (useAbsoluteId) {
            extensionsProgramDao.getAllProgramByChannelId(queryChannelId)
                .toObservable()
        } else {
            extensionsProgramDao.getAllLikeChannelId(queryChannelId)
                .toObservable()
        }
    }

    fun getCurrentProgramForTVChannel(
        channelId: String
    ): Observable<TVScheduler.Programme> {
        return getCurrentProgramForChannel(
            channelId,
            useAbsoluteId = false,
            filterTimestamp = true
        )
    }

    fun getCurrentProgramForExtensionChannel(
        channel: ExtensionsChannel
    ): Observable<TVScheduler.Programme> {
        return getCurrentProgramForChannel(
            channel.channelId,
            useAbsoluteId = true,
            filterTimestamp = false
        )
    }

    private fun getCurrentProgramForChannel(
        tvChannelId: String,
        useAbsoluteId: Boolean,
        filterTimestamp: Boolean
    ): Observable<TVScheduler.Programme> {
        val currentTime: Long = Calendar.getInstance(Locale.getDefault())
            .timeInMillis
        return getListProgramForChannel(
            tvChannelId, useAbsoluteId
        ).flatMapIterable {
            it
        }.filter {
            if (filterTimestamp) {
                val pattern = if (it.start.contains("+0700")) {
                    DATE_TIME_FORMAT_0700
                } else {
                    DATE_TIME_FORMAT
                }
                val start: Long = it.start.toDate(
                    pattern,
                    Locale.getDefault(),
                    false
                )?.time ?: return@filter false

                val patternStop = if (it.stop.contains("+0700")) {
                    DATE_TIME_FORMAT_0700
                } else {
                    DATE_TIME_FORMAT
                }
                val stop: Long = it.stop.toDate(
                    patternStop,
                    Locale.getDefault(),
                    false
                )?.time ?: return@filter false
                (start <= currentTime) && stop >= currentTime
            } else {
                true
            }
        }
    }

    fun parseForConfig(config: ExtensionsConfig) {
        disposable.add(
            roomDataBase.tvSchedulerDao()
                .getAllByExtensionlId(config.sourceUrl)
                .toObservable()
                .flatMapIterable {
                    it
                }
                .flatMapCompletable {
                    parseForConfigRx(config, it.epgUrl)
                }
                .subscribe({

                }, {

                })
        )
    }

    fun parseForConfig(config: ExtensionsConfig, programScheduleUrl: String) {
        disposable.add(
            parseForConfigRx(config, programScheduleUrl)
                .subscribe({
                    Logger.d(this@ParserExtensionsProgramSchedule, message = "Complete")
                }, {
                    Logger.e(this@ParserExtensionsProgramSchedule, exception = it)
                })
        )
    }

    fun parseForConfigRx(config: ExtensionsConfig, programScheduleUrl: String): Completable {
        return Observable.fromArray(programScheduleUrl.split(",")
            .filter {
                it.trim().isNotBlank()
            })
            .flatMapIterable {
                it
            }
            .concatMapCompletable { url ->
                getListTvProgramRx(config, url)
            }
            .observeOn(pool)
    }

    private fun getListTvProgramRx(config: ExtensionsConfig, programScheduleUrl: String) =
        getListTvProgram(config, programScheduleUrl).observeOn(pool)
            .subscribeOn(pool)
            .retry { times, throwable ->
                Logger.e(this@ParserExtensionsProgramSchedule, message = "retry - $programScheduleUrl")
                return@retry times < 3 && throwable !is CannotRetryThrowable
            }
            .doOnComplete {
                Logger.e(this@ParserExtensionsProgramSchedule, message = "$programScheduleUrl - Complete insert")
            }.doOnError {
                Logger.e(this@ParserExtensionsProgramSchedule, message = "$programScheduleUrl - Error")
                Logger.e(this@ParserExtensionsProgramSchedule, exception = it)
            }

    private fun getListTvProgram(
        config: ExtensionsConfig,
        programScheduleUrl: String
    ) = Observable.create { emitter ->
        val networkCall = client.newCall(
            Request.Builder()
                .url(programScheduleUrl)
                .addHeader("Content-Type", "text/xml")
                .build()
        ).execute()

        Logger.d(this@ParserExtensionsProgramSchedule, "Url", programScheduleUrl)

        when (networkCall.code) {
            in 200..299 -> {
            }

            403, 404, 502 -> {
                throw InvalidOrNotFoundUrlThrowable("Cannot retry")
            }

            else -> {
                throw Throwable(networkCall.message)
            }
        }

        val responseStr = networkCall.body
        val stream = if (networkCall.headers["content-type"] == "application/octet-stream") {
            GZIPInputStream(responseStr.source().inputStream())
        } else {
            responseStr.source().inputStream()
        }
        val node: InputNode = try {
            NodeBuilder.read(stream)
        } catch (e: Exception) {
            emitter.onNext(InvalidFormatThrowable("Cannot retry"))
            return@create
        }
        var readNode: InputNode? = node.next
        var channel: TVScheduler.Channel
        var listChannel = mutableListOf<TVScheduler.Channel>()
        var programme: TVScheduler.Programme
        var listProgram = mutableListOf<TVScheduler.Programme>()
        var tvScheduler: TVScheduler
        while (readNode != null) {
            when {
                readNode.name.trim() == "tv" -> {
                    tvScheduler = TVScheduler()
                    try {
                        tvScheduler.extensionsConfigId = config.sourceUrl
                        tvScheduler.epgUrl = programScheduleUrl
                        tvScheduler.generatorInfoName = readNode.attributes.get("generator-info-name")?.value ?: ""
                        tvScheduler.generatorInfoUrl = readNode.attributes.get("generator-info-url")?.value ?: ""
                    } catch (_: Exception) {
                    }

                    emitter.onNext(tvScheduler)
                }

                readNode.isRoot -> {
                }


                readNode.name.trim() == "channel" -> {
                    channel = TVScheduler.Channel()
                    channel.id = readNode.attributes.get("id").value
                    readNode = node.next
                    while (readNode != null
                        && readNode.name.trim() != "channel"
                        && readNode.name.trim() != "programme"
                    ) {
                        when (readNode.name.trim()) {
                            "display-name" -> {
                                channel.displayName = readNode.value ?: ""
                            }

                            "display-number" -> {
                                channel.displayNumber = readNode.value ?: ""
                            }

                            "icon" -> {
                                channel.icon = readNode.attributes.get("src")?.value ?: ""

                            }
                        }
                        readNode = node.next
                    }
                    listChannel.add(channel)
                    if (listChannel.size > 50) {
                        listChannel = mutableListOf()
                    }

                    if (readNode != null) {
                        continue
                    }
                }

                readNode.name.trim() == "programme" -> {
                    programme = TVScheduler.Programme()
                    programme.extensionEpgUrl = programScheduleUrl
                    programme.extensionsConfigId = config.sourceUrl
                    programme.channel = readNode.attributes.get("channel").value ?: ""
                    programme.start = readNode.attributes.get("start").value ?: ""
                    programme.stop = readNode.attributes.get("stop").value ?: ""
                    readNode = node.next
                    var nodeName = readNode.name.trim()
                    while (readNode != null
                        && nodeName != "channel"
                        && nodeName != "programme"
                    ) {
                        when (nodeName) {
                            "title" -> {
                                programme.title = readNode.value ?: ""
                            }

                            "desc" -> {
                                programme.description = readNode.value ?: ""
                            }
                        }
                        readNode = node.next
                        nodeName = readNode.name?.trim() ?: ""
                    }
                    listProgram.add(programme)
                    if (listProgram.size > 50) {
                        emitter.onNext(listProgram)
                        listProgram = mutableListOf()
                    }
                    if (readNode != null) {
                        continue
                    }
                }
            }
            readNode = node.next
        }

        if (listProgram.isNotEmpty()) {
            emitter.onNext(listProgram)
        }
        emitter.onComplete()
    }.concatMapCompletable {
        when (it) {
            is TVScheduler -> {
                Logger.d(
                    this@ParserExtensionsProgramSchedule,
                    "TVScheduler",
                    message = "$it"
                )
                roomDataBase.tvSchedulerDao()
                    .insert(it)
            }

            is List<*> -> {
                when (it.first()) {
                    is TVScheduler.Programme -> {
                        Logger.d(
                            this@ParserExtensionsProgramSchedule,
                            message = "TVScheduler.Programme: ${it.size}"
                        )
                        extensionsProgramDao.insert(it as List<TVScheduler.Programme>)
                    }

                    else -> {
                        Completable.complete()
                    }
                }
            }

            else -> {
                Completable.complete()
            }
        }
    }


    private class InvalidOrNotFoundUrlThrowable(
        override val message: String? = ""
    ) : CannotRetryThrowable(message) {
    }

    private class InvalidFormatThrowable(
        override val message: String? = ""
    ) : CannotRetryThrowable(message) {
    }

    private open class CannotRetryThrowable(
        override val message: String? = ""
    ) : Throwable(message) {
    }
}
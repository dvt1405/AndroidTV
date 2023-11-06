package com.kt.apps.core.extensions

import android.text.format.DateUtils
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.Constants
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
import org.json.JSONArray
import org.simpleframework.xml.stream.InputNode
import org.simpleframework.xml.stream.NodeBuilder
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
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    @Named(NetworkModule.EXTRA_NETWORK_DISPOSABLE)
    private val disposable: DisposableContainer
) {
    private val pool by lazy {
        Schedulers.io()
    }

    private val extensionsProgramDao by lazy {
        roomDataBase.extensionsTVChannelProgramDao()
    }

    private val _tvSchedulerDao = roomDataBase.tvSchedulerDao()

    private val pendingSource by lazy {
        mutableMapOf<String, Completable>()
    }

    private val pendingSourceStatus by lazy {
        mutableMapOf<String, PendingSourceStatus>()
    }
    private val mappingEpgId by lazy {
        mutableMapOf<String, String>()
    }
    fun getRelatedProgram(channel: TVChannelDTO) = getMappingEpgChannelId()[channel.channelId]?.split("|")?.map { newId ->
        getListProgramForTVChannel(newId, true)
            .map {
                it.map {
                    TVScheduler.Programme(
                        channel = channel.channelId,
                        channelNumber = it.channelNumber,
                        start = it.start,
                        stop = it.stop,
                        title = it.title,
                        description = it.description,
                        extensionsConfigId = it.extensionsConfigId,
                        extensionEpgUrl = it.extensionEpgUrl
                    )
                }
            }
    }?.reduce { acc, observable ->
        acc.mergeWith(observable)
    }

    fun getMappingEpgChannelId(): Map<String, String> {
        try {
            if (mappingEpgId.isNotEmpty()) return mappingEpgId
            val remoteMapping = firebaseRemoteConfig.getString("tv_epg_mapping")
            val jsonArr = JSONArray(remoteMapping)
            Logger.d(this@ParserExtensionsProgramSchedule, message = "{\"RemoteMapping\": $remoteMapping}")
            for (i in 0 until jsonArr.length()) {
                val key = jsonArr.optJSONObject(i)?.optString("key") ?: continue
                val value = jsonArr.optJSONObject(i)?.optString("value") ?: continue
                mappingEpgId[key] = value
            }
            if (mappingEpgId.isNotEmpty()) {
                return mappingEpgId
            }
        } catch (_: Exception) {
        }
        return Constants.mapping
    }

    private enum class PendingSourceStatus {
        PENDING,
        RUNNING,
        DONE,
        ERROR
    }

    fun getListProgramForExtensionsChannel(
        channel: ExtensionsChannel
    ): Observable<List<TVScheduler.Programme>> {
        return getListProgramForChannel(channel.channelId, true)
    }

    fun getListProgramForTVChannel(
        tvChannelId: String,
        useAbsoluteId: Boolean = false
    ): Observable<List<TVScheduler.Programme>> {
        return getListProgramForChannel(tvChannelId, useAbsoluteId)
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
        channel: ExtensionsChannel,
        configType: ExtensionsConfig.Type
    ): Observable<TVScheduler.Programme> {
        return getCurrentProgramForChannel(
            channel.channelId,
            useAbsoluteId = true,
            filterTimestamp = configType == ExtensionsConfig.Type.TV_CHANNEL
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
                val start: Long = if (it.start.trim() == "+0700") {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.timeInMillis
                } else {
                    it.start.toDate(
                        pattern,
                        Locale.getDefault(),
                        false
                    )?.time ?: return@filter false
                }

                val patternStop = if (it.stop.contains("+0700")) {
                    DATE_TIME_FORMAT_0700
                } else {
                    DATE_TIME_FORMAT
                }
                val stop: Long = if (it.stop.trim() == "+0700") {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DATE, 1)
                    calendar.set(Calendar.HOUR, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.timeInMillis
                } else {
                    it.stop.toDate(
                        patternStop,
                        Locale.getDefault(),
                        false
                    )?.time ?: return@filter false
                }
                if (!DateUtils.isToday(start) && !DateUtils.isToday(stop)) return@filter false
                currentTime in start..stop
            } else {
                true
            }
        }.doOnNext {
            Logger.d(this@ParserExtensionsProgramSchedule, tag = "Epg" ,message = "$it")
        }
    }

    fun parseForConfig(config: ExtensionsConfig) {
        disposable.add(
            _tvSchedulerDao.getAllByExtensionlId(config.sourceUrl)
                .toObservable()
                .flatMapIterable {
                    it
                }
                .flatMapCompletable {
                    parseForConfigRx(config, it.epgUrl)
                }
                .subscribe({
                    Logger.d(this@ParserExtensionsProgramSchedule, message = "Complete epg: {" +
                            "config: ${config.sourceUrl}, }")
                }, {
                    Logger.e(this@ParserExtensionsProgramSchedule, exception = it)
                })
        )
    }

    fun runPendingSource() {
        if (pendingSource.isEmpty()) {
            return
        }
        val sources = pendingSource.filter {
            pendingSourceStatus[it.key] == PendingSourceStatus.PENDING
        }.takeIf {
            it.isNotEmpty()
        } ?: return

        synchronized(sources) {
            var sourceCount = 0
            for ((key, source) in sources.entries) {
                disposable.add(
                    source.subscribe({
                        Logger.d(this@ParserExtensionsProgramSchedule, message = "Complete")
                    }, {
                        Logger.e(this@ParserExtensionsProgramSchedule, exception = it)
                    })
                )
                pendingSourceStatus[key] = PendingSourceStatus.RUNNING
                sourceCount++
                if (sourceCount > 3) {
                    break
                }
            }
        }
    }

    fun appendParseForConfigTask(config: ExtensionsConfig, programScheduleUrl: String) {
        programScheduleUrl.split(",")
            .filter {
                it.trim().isNotBlank()
            }
            .forEach { url ->
                if (
                    pendingSource[url] == null ||
                    (pendingSourceStatus[url] != PendingSourceStatus.RUNNING &&
                            pendingSourceStatus[url] != PendingSourceStatus.PENDING)
                ) {
                    pendingSource[url] = getListTvProgramRx(config, url)
                    pendingSourceStatus[url] = PendingSourceStatus.PENDING
                }
            }
    }

    fun parseForConfigRx(config: ExtensionsConfig, tvgUrlList: String): Completable {
        return Observable.fromArray(tvgUrlList.split(",")
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
        extensionsProgramDao.deleteProgramByConfig(config.sourceUrl, programScheduleUrl)
            .observeOn(pool)
            .subscribeOn(pool)
            .onErrorComplete()
            .andThen(getListTvProgram(config, programScheduleUrl).observeOn(pool))
            .subscribeOn(pool)
            .retry { times, throwable ->
                Logger.e(this@ParserExtensionsProgramSchedule, message = "retry - $programScheduleUrl")
                return@retry times < 3 && throwable !is CannotRetryThrowable
            }
            .doOnComplete {
                Logger.e(this@ParserExtensionsProgramSchedule, message = "$programScheduleUrl - Complete insert")
                removePendingSource(programScheduleUrl)
                runPendingSource()
            }.doOnError {
                Logger.e(this@ParserExtensionsProgramSchedule, message = "$programScheduleUrl - Error")
                Logger.e(this@ParserExtensionsProgramSchedule, exception = it)
                removePendingSource(programScheduleUrl)
                runPendingSource()
            }

    private fun removePendingSource(programScheduleUrl: String) {
        synchronized(pendingSource) {
            pendingSource.remove(programScheduleUrl)
            pendingSourceStatus.remove(programScheduleUrl)
        }
    }

    private fun getListTvProgram(
        config: ExtensionsConfig,
        programScheduleUrl: String
    ) = Observable.create<Any> { emitter ->
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
            if (emitter.isDisposed) return@create
            emitter.onError(InvalidFormatThrowable("Cannot retry"))
            return@create
        }
        var readNode: InputNode? = node.next
        var channel: TVScheduler.Channel
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

                    if (emitter.isDisposed) return@create
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
                        nodeName = if (readNode != null) {
                            readNode.name?.trim() ?: ""
                        } else {
                            ""
                        }
                    }
                    listProgram.add(programme)
                    if (emitter.isDisposed) return@create
                    if (listProgram.size > 150) {
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

        if (emitter.isDisposed) return@create
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
                _tvSchedulerDao
                    .insert(it)
                    .onErrorComplete()
            }

            is List<*> -> {
                if (it.isEmpty()) {
                    return@concatMapCompletable Completable.complete()
                }

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

    fun clearCache() {
        mappingEpgId.clear()
    }

    init {
        instance = this
    }

    companion object {
        private var instance: ParserExtensionsProgramSchedule? = null

        @Synchronized
        fun getInstance(): ParserExtensionsProgramSchedule? = instance
    }
}
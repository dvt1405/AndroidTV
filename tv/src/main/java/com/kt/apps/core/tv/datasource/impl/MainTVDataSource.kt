package com.kt.apps.core.tv.datasource.impl

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.getIsVipDb
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.TVChannelWithUrls
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.needRefreshData
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import com.kt.apps.core.utils.toOrigin
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

@TVScope
class MainTVDataSource @Inject constructor(
    private val sctvDataSource: SCTVDataSourceImpl,
    private val vDataSourceImpl: VDataSourceImpl,
    private val hyDataSourceImpl: HYDataSourceImpl,
    private val vtvDataSourceImpl: VTVBackupDataSourceImpl,
    private val vtcDataSourceImpl: VtcBackupDataSourceImpl,
    private val vovDataSourceImpl: VOVDataSourceImpl,
    private val htvDataSourceImpl: HTVBackUpDataSourceImpl,
    private val onLiveDataSourceImpl: OnLiveDataSourceImpl,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val tvStorage: Provider<TVStorage>,
    private val roomDataBase: RoomDataBase,
    private val context: Context
) : ITVDataSource {
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    private val needRefresh: Boolean
        get() = this.needRefreshData(firebaseRemoteConfig, tvStorage.get())

    private val versionNeedRefresh: Long
        get() = firebaseRemoteConfig.getLong(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)

    private val allowInternational: Boolean
        get() = firebaseRemoteConfig.getBoolean(Constants.EXTRA_KEY_ALLOW_INTERNATIONAL)

    private var isVipDb: Boolean = false

    private fun checkVipDb() {
        isVipDb = tvStorage.get().getIsVipDb()
    }
    override fun getTvList(): Observable<List<TVChannel>> {
        if (!isVipDb) {
            checkVipDb()
        }
        val onlineSource = if (context.packageName.contains("mobile")) {
            getFirebaseSource(Platform.MOBILE)
        } else {
            getFirebaseSource(Platform.TV)
        }.reduce { t1, t2 ->
            t1.toMutableList().let {
                it.addAll(t2)
                it
            }
        }.toObservable()

        val dataBaseSource = roomDataBase.tvChannelDao()
            .getListChannelWithUrl()
            .flatMapObservable {
                if (it.isEmpty()) {
                    onlineSource
                } else {
                    if (it.first().tvChannel.searchKey.isEmpty()) {
                        saveToRoomDB(it.mapToTVChannel())
                    }
                    Logger.d(this@MainTVDataSource, message = "Offline source: ${Gson().toJson(it)}")
                    Observable.just(it.mapToTVChannel()
                        .sortedBy(ITVDataSource.sortTVChannel()))
                }
            }

        if (!needRefresh) {
            return dataBaseSource
                .onErrorResumeNext {
                    Logger.e(this@MainTVDataSource, exception = it)
                    if (it.message == "EmptyData") {
                        onlineSource
                    } else {
                        Observable.error(it)
                    }
                }
        }

        return onlineSource
    }

    private fun getFirebaseSource(
        platform: Platform
    ): Observable<List<TVChannel>> = Observable.create { emitter ->
        Logger.d(this@MainTVDataSource, message = "getFirebaseSource")
        val totalList = getChannelListFromFirebase(platform, isVipDb)
            .sortedBy(ITVDataSource.sortTVChannel())
        if (emitter.isDisposed) {
            return@create
        }
        emitter.onNext(totalList)
        emitter.onComplete()
    }.retry { t1, t2 ->
        Logger.d(this@MainTVDataSource, message = "Retry: $t1, $t2")
        Thread.sleep(1_500)
        t1 < 3
    }

    private fun getChannelListFromFirebase(
        platform: Platform,
        isVip: Boolean
    ): List<TVChannel> {
        var saveCache = true
        var listTVChannelJs = if (platform == Platform.MOBILE) {
            firebaseRemoteConfig.getString("remote_channel_list_mobile")
        } else {
            firebaseRemoteConfig.getString("remote_channel_list_tv")
        }
        Logger.d(this@MainTVDataSource, message = "getChannelListFromFirebase: $listTVChannelJs")
        if (listTVChannelJs.isEmpty()) {
            saveCache = false
            listTVChannelJs = context.assets.open("default_channel_list.json")
                .bufferedReader().use {
                    it.readText()
                }
        }

        val channelList = Gson().fromJson<List<TVChannelFromDB>>(
            JSONObject(listTVChannelJs).getJSONArray(ALL_CHANNEL_NAME).toString(),
            TypeToken.getParameterized(
                List::class.java,
                TVChannelFromDB::class.java
            ).type
        ).mapToListChannel()
        if (saveCache) {
            saveToRoomDB(channelList)
            tvStorage.get().saveRefreshInVersion(
                Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
                versionNeedRefresh
            )
        }

        return when {
            isVip -> {
                channelList
            }

            !allowInternational -> {
                channelList.filter {
                    it.tvGroup != TVChannelGroup.Intenational.name &&
                            (it.tvGroup != TVChannelGroup.Kid.name)
                }
            }

            else -> {
                channelList.filter {
                    (it.tvGroup != TVChannelGroup.Intenational.name) &&
                            (it.tvGroup != TVChannelGroup.Kid.name) &&
                            (it.tvGroup != TVChannelGroup.VTVCAB.name)

                }
            }
        }
    }

    private fun saveToRoomDB(tvDetails: List<TVChannel>) {
        val tvUrl = mutableListOf<TVChannelDTO.TVChannelUrl>()
        val tvChannelList = mutableListOf<TVChannelDTO>()
        tvDetails.forEach { channel ->

            channel.urls.forEach {
                tvUrl.add(
                    TVChannelDTO.TVChannelUrl(
                        src = it.dataSource,
                        type = it.type,
                        url = it.url,
                        tvChannelId = channel.channelId
                    )
                )
            }

            tvChannelList.add(
                TVChannelDTO(
                    channel.tvGroup,
                    channel.logoChannel,
                    channel.tvChannelName,
                    sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                    channel.channelId,
                    channel.tvChannelName.lowercase()
                        .replaceVNCharsToLatinChars()
                        .removeAllSpecialChars()
                )
            )
        }

        val source1 = roomDataBase.tvChannelDao()
            .insertListChannel(
                tvChannelList
            )

        val source2 = roomDataBase.tvChannelUrlDao()
            .insert(tvUrl)


        compositeDisposable.add(
            Completable.concatArray(source1, source2)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Logger.d(this@MainTVDataSource, message = "Insert source success")
                }, {
                    Logger.e(this@MainTVDataSource, exception = it)
                })
        )
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        Logger.d(this@MainTVDataSource, message = "getTVFromDetail: ${Gson().toJson(tvChannel)}, isBackup: $isBackup")

        val streamingUrl = tvChannel.urls
            .filter {
                it.url.isNotBlank()
            }
            .filter {
                it.type.lowercase() == TVChannelUrlType.STREAM.value
            }
            .map {
                val newUrl = it.url.replace("{uuid}", UUID.randomUUID().toString().replace("-", ""))
                    .replace("{time_stamp}", "${System.currentTimeMillis()}")
                    .replace("{time_stamp_second}", "${System.currentTimeMillis() / 1000}")
                it.copy(url = newUrl)
            }
            .map {
                if (it.url.contains("|Referer")) {
                    val refererIndex = it.url.indexOf("|Referer")
                    val newUrl = it.url.substring(0, refererIndex)
                    it.url = newUrl
                    it
                } else {
                    it
                }
            }

        val webUrl = tvChannel.urls
            .filter {
                it.url.isNotBlank() && it.type.lowercase() == TVChannelUrlType.WEB_PAGE.value
            }.toMutableList()

        if (webUrl.isNotEmpty()) {
            return if (!isBackup) {
                Observable.just(webUrl.first())
                    .flatMap {
                        getStreamUrlByWebUrlAndSrc(it, tvChannel, false)
                    }
                    .switchIfEmpty { observer ->
                        observer.onError(Throwable("EmptyData"))
                    }.retry(1)
            } else {
                if (webUrl.size > 1) {
                    if (streamingUrl.isNotEmpty()) {
                        webUrl.addAll(streamingUrl)
                    }
                    webUrl.forEach {
                        Logger.d(this@MainTVDataSource, message = "$it")
                    }

                    Observable.fromIterable(webUrl).flatMap {
                        if (it.type == TVChannelUrlType.WEB_PAGE.value) {
                            getStreamUrlByWebUrlAndSrc(it, tvChannel, false)
                        } else {
                            Observable.just(streamingUrl.mapToLinkStream(tvChannel))
                        }
                    }.reduce { left, right ->
                        val finalStreamList = left.linkStream.toMutableList()
                        if (right.linkStream.isNotEmpty()) {
                            finalStreamList.addAll(right.linkStream)
                        }
                        return@reduce left.copy(
                            linkStream = finalStreamList.distinct()
                        )
                    }.toObservable()
                        .doOnNext {
                            Logger.d(this@MainTVDataSource, message = "getTVFromDetail: $it")
                        }

                } else {
                    Observable.just(streamingUrl.mapToLinkStream(tvChannel))
                }
            }
        }

        return Observable.just(
            streamingUrl.mapToLinkStream(tvChannel)
        )
    }

    private fun List<TVChannel.Url>.mapToLinkStream(channel: TVChannel): TVChannelLinkStream {
        return TVChannelLinkStream(
            channel,
            this.map {
                TVChannel.Url.fromUrl(
                    it.url,
                    type = TVChannelUrlType.STREAM.value,
                    referer = it.referer ?: it.url.toHttpUrlOrNull()?.toOrigin() ?: it.url
                )
            }
        )
    }

    private fun getStreamUrlByWebUrlAndSrc(
        url: TVChannel.Url,
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        val backupChannel = tvChannel.copy(
            tvChannelWebDetailPage = url.url,
            referer = url.url
        )
        Logger.d(this@MainTVDataSource, message = "getStreamUrlByWebUrlAndSrc: $url")
        Logger.d(this@MainTVDataSource, message = "getStreamUrlByWebUrlAndSrc: $tvChannel")
        return when (url.dataSource) {
            TVChannelURLSrc.VTV.value -> {
                vtvDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.ON_LIVE.value -> {
                onLiveDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.VOV.value -> {
                vovDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.HTV.value -> {
                htvDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.VTC.value -> {
                vtcDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.HY.value -> {
                hyDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.SCTV.value -> {
                sctvDataSource.getTvLinkFromDetail(backupChannel, isBackup)
            }

            TVChannelURLSrc.V.value -> {
                vDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }

            else -> {
                vDataSourceImpl.getTvLinkFromDetail(backupChannel, isBackup)
            }
        }.switchIfEmpty { observer ->
            Logger.d(this@MainTVDataSource, message = "Empty Data")
            observer.onError(Throwable("Empty Data"))
        }.onErrorResumeNext {
            val nextUrl = findNextUrl(url, tvChannel)
            if (nextUrl != null) {
                Logger.d(this@MainTVDataSource, message = "NexUrl: $nextUrl")
                getStreamUrlByWebUrlAndSrc(nextUrl, tvChannel, false)
            } else {
                Observable.error(it)
            }
        }
    }

    private fun findNextUrl(url: TVChannel.Url, tvChannel: TVChannel): TVChannel.Url? {
        val currentIndex = tvChannel.urls.map {
            it.url
        }.indexOf(url.url)

        if (currentIndex == tvChannel.urls.size - 1) {
            return null
        }

        for (i in currentIndex + 1 until tvChannel.urls.size) {
            if (tvChannel.urls[i].type == TVChannelUrlType.WEB_PAGE.value) {
                return tvChannel.urls[i]
            }
        }
        return null
    }

    data class TVChannelFromDB @JvmOverloads constructor(
        var group: String = "",
        var id: String = "",
        var isRadio: Boolean? = false,
        var name: String = "",
        var thumb: String = "",
        var urls: List<Url?> = listOf()
    ) {
        data class Url @JvmOverloads constructor(
            val src: String? = null,
            val type: String = "",
            val url: String = ""
        )
    }

    enum class TVChannelUrlType(val value: String) {
        STREAM("streaming"), WEB_PAGE("web")
    }

    enum class TVChannelURLSrc(val value: String) {
        V("vieon"), SCTV("sctv"), HY("hy"), VTV("vtv"),
        ON_LIVE("on_live"), VOV("vov"), HTV("htv"), VTC("vtc")
    }

    enum class Platform(val value: String) {
        MOBILE("mobile"), TV("tv")
    }

    companion object {
        private const val ALL_CHANNEL_NAME = "AllChannels"
        private val supportGroups by lazy {
            listOf(
                TVChannelGroup.VTV,
                TVChannelGroup.HTV,
                TVChannelGroup.SCTV,
                TVChannelGroup.VTC,
                TVChannelGroup.THVL,
                TVChannelGroup.AnNinh,
                TVChannelGroup.HTVC,
                TVChannelGroup.DiaPhuong,
                TVChannelGroup.Intenational,
                TVChannelGroup.Kid,

                TVChannelGroup.VOV,
                TVChannelGroup.VOH,
            )
        }
        fun List<TVChannelFromDB>.mapToListChannel(): List<TVChannel> {
            return this.map {
                it.mapToTVChannel()
            }
        }

        fun List<TVChannelWithUrls>.mapToTVChannel(): List<TVChannel> {
            return this.map {
                it.mapToTVChannel()
            }
        }
        fun TVChannelWithUrls.mapToTVChannel(): TVChannel {
            return TVChannel(
                tvGroup = this.tvChannel.tvGroup,
                tvChannelName = this.tvChannel.tvChannelName,
                tvChannelWebDetailPage = this.urls.firstOrNull {
                    it.type == TVChannelUrlType.WEB_PAGE.value
                }?.url ?: this.urls[0].url,
                urls = this.urls.map { url ->
                    TVChannel.Url(
                        dataSource = url.src,
                        url = url.url,
                        type = url.type
                    )
                },
                sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                logoChannel = this.tvChannel.logoChannel,
                channelId = this.tvChannel.channelId
            )
        }
        fun TVChannelFromDB.mapToTVChannel(): TVChannel {
            val totalUrls = this.urls.filterNotNull()
            return TVChannel(
                tvGroup = this.group,
                tvChannelName = this.name,
                tvChannelWebDetailPage = totalUrls.firstOrNull {
                    it.type == TVChannelUrlType.WEB_PAGE.value
                }?.url ?: totalUrls[0].url,
                urls = totalUrls
                    .map { url ->
                        TVChannel.Url(
                            dataSource = url.src,
                            url = url.url,
                            type = url.type
                        )
                    },
                sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                logoChannel = this.thumb,
                channelId = this.id
            )
        }
    }
}
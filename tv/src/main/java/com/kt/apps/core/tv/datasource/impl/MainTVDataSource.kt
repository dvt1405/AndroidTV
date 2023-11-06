package com.kt.apps.core.tv.datasource.impl

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider

@TVScope
class MainTVDataSource @Inject constructor(
    private val sctvDataSource: SCTVDataSourceImpl,
    private val vDataSourceImpl: VDataSourceImpl,
    private val hyDataSourceImpl: HYDataSourceImpl,
    private val vtvDataSourceImpl: VTVBackupDataSourceImpl,
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val fireStoreDataBase: FirebaseFirestore,
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


    override fun getTvList(): Observable<List<TVChannel>> {
        val onlineSource = if (context.packageName.contains("mobile")) {
            getFireStoreSource().onErrorResumeNext { newGetFirebaseSource() }
        } else {
            newGetFirebaseSource()
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

    private fun getFireStoreSource(): Observable<List<TVChannel>> {
        return Observable.create<List<TVChannel>> { emitter ->
            fireStoreDataBase.collection("tv_channels_by_version")
                .document("1")
                .get()
                .addOnSuccessListener {
                    val jsonObject = JSONObject(it.data!!["alls"]!!.toString())
                    val totalList = mutableListOf<TVChannel>()
                    supportGroups.forEach {
                        val listJsonArr = jsonObject.optJSONArray(it.name)
                        if (listJsonArr != null && listJsonArr.length() > 0) {
                            val list = Gson().fromJson<List<TVChannelFromDB?>>(
                                listJsonArr.toString(),
                                TypeToken.getParameterized(List::class.java, TVChannelFromDB::class.java).type
                            ).filterNotNull()
                            if (list.isNotEmpty()) {
                                totalList.addAll(
                                    list.mapToListChannel()
                                        .sortedBy(ITVDataSource.sortTVChannel())
                                        .filter {
                                            if (!allowInternational) {
                                                it.tvGroup != TVChannelGroup.Intenational.name &&
                                                        (it.tvGroup != TVChannelGroup.Kid.name)
                                            } else {
                                                true
                                            }
                                        }
                                )
                            }
                        }
                    }
                    saveToRoomDB(totalList)
                    fireStoreDataBase.clearPersistence()
                    emitter.onNext(totalList)
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    if (!emitter.isDisposed) {
                        emitter.onError(it)
                    }
                }
        }.retry { t1, t2 ->
            t1 < 3
        }
    }

    private fun getFirebaseSource(): Observable<List<TVChannel>> =
        Observable.create<List<TVChannel>> { emitter ->
            val totalGroupChannels = supportGroups.toMutableList()
            if (!allowInternational) {
                totalGroupChannels.remove(TVChannelGroup.Intenational)
                totalGroupChannels.remove(TVChannelGroup.Kid)
            }
            val totalGroup = totalGroupChannels.size
            val totalList = mutableListOf<TVChannel>()
            var totalCount = 0
            Logger.d(this@MainTVDataSource, message = "getFirebaseSource")

            totalGroupChannels.forEach { group ->
                firebaseDatabase.reference
                    .child(ALL_CHANNEL_NAME)
                    .ref
                    .child(group.name)
                    .get()
                    .addOnSuccessListener {
                        totalCount++
                        val value = it.getValue<List<TVChannelFromDB?>>() ?: return@addOnSuccessListener
                        val tvList = value.filterNotNull()
                            .mapToListChannel()
                            .sortedBy(ITVDataSource.sortTVChannel())

                        totalList.addAll(tvList)
                        emitter.onNext(tvList)
                        saveToRoomDB(tvList)
                        Logger.d(this@MainTVDataSource, message = "Counter: $totalCount, $totalGroup")
                        if (totalCount == totalGroup) {
                            emitter.onComplete()
                            tvStorage.get().saveRefreshInVersion(
                                Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
                                versionNeedRefresh
                            )
                        }
                    }
                    .addOnFailureListener {
                        Logger.e(this@MainTVDataSource, exception = it)
                        totalCount++
                        if (totalCount == totalGroup && totalList.isEmpty()) {
                            emitter.onNext(totalList)
                            emitter.onComplete()
                        } else {
                            emitter.onError(it)
                        }
                    }

            }
        }.retry { t1, t2 ->
            t1 < 3
        }

    private fun newGetFirebaseSource(): Observable<List<TVChannel>> =
        Observable.create<List<TVChannel>> { emitter ->
            Logger.d(this@MainTVDataSource, message = "getFirebaseSource")
            val totalGroupChannels = supportGroups.toMutableList()
            if (!allowInternational) {
                totalGroupChannels.remove(TVChannelGroup.Intenational)
                totalGroupChannels.remove(TVChannelGroup.Kid)
            }
            var dataSnapshot: DataSnapshot? = null
            val isSuccess = AtomicBoolean(false)
            firebaseDatabase.reference.child(ALL_CHANNEL_NAME)
                .ref.get()
                .addOnSuccessListener {
                    dataSnapshot = it
                    isSuccess.set(true)
                }
                .addOnFailureListener {
                    isSuccess.set(true)
                    emitter.onError(it)
                }
            while (!isSuccess.get()) {
                Thread.sleep(500)
                if (emitter.isDisposed) {
                    return@create
                }
            }

            if (dataSnapshot == null) {
                emitter.onError(Throwable("EmptyData"))
                return@create
            }

            val totalList = totalGroupChannels.mapNotNull {
                dataSnapshot?.child(it.name)?.getValue<List<TVChannelFromDB?>>()
                    ?.filterNotNull()
                    ?.mapToListChannel()
            }
                .flatten()
                .sortedBy(ITVDataSource.sortTVChannel())
            saveToRoomDB(totalList)
            tvStorage.get().saveRefreshInVersion(
                Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
                versionNeedRefresh
            )
            emitter.onNext(totalList)
            emitter.onComplete()
        }.retry { t1, t2 ->
            Logger.d(this@MainTVDataSource, message = "Retry: $t1, $t2")
            Thread.sleep(1_500)
            t1 < 3
        }

    private fun getFirebaseSourceVip(): Observable<List<TVChannel>> = Observable.create { emitter ->
        var dataSnapshot: DataSnapshot? = null
        val isSuccess = AtomicBoolean(false)
        firebaseDatabase.reference.child("AllChannels")
            .ref.get()
            .addOnSuccessListener {
                dataSnapshot = it
                isSuccess.set(true)
            }
            .addOnFailureListener {
                isSuccess.set(true)
                emitter.onError(it)
            }
        while (!isSuccess.get()) {
            Thread.sleep(500)
            if (emitter.isDisposed) {
                return@create
            }
        }
        val childValue = dataSnapshot!!.getValue<List<TVChannelFromDB?>>()
        val tvList = childValue!!.filterNotNull()
            .mapToListChannel()
            .sortedBy(ITVDataSource.sortTVChannel())
        saveToRoomDB(tvList)
        tvStorage.get().saveRefreshInVersion(
            Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
            versionNeedRefresh
        )
        emitter.onNext(tvList)
        emitter.onComplete()
    }.retry { t1, t2 ->
        t1 < 3
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
                    }.map { finalTVWithLinkStream ->
                        if (streamingUrl.isNotEmpty()) {
                            val newListStreaming = finalTVWithLinkStream.linkStream.toMutableList()
                            newListStreaming.addAll(streamingUrl.map {
                                TVChannel.Url.fromUrl(it.url, type = TVChannelUrlType.STREAM.value)
                            })
                            webUrl.removeAt(0)
                            newListStreaming.addAll(webUrl)
                            return@map TVChannelLinkStream(
                                finalTVWithLinkStream.channel,
                                newListStreaming.distinct()
                            )
                        }
                        return@map finalTVWithLinkStream
                    }
            } else {
                if (webUrl.size > 1) {
                    webUrl.removeAt(0)
                    Observable.fromIterable(webUrl)
                        .flatMap {
                            getStreamUrlByWebUrlAndSrc(it, tvChannel, false)
                        }

                } else {
                    Observable.just(
                        TVChannelLinkStream(
                            tvChannel,
                            streamingUrl.map {
                                TVChannel.Url.fromUrl(it.url, type = TVChannelUrlType.STREAM.value)
                            }
                        )
                    )
                }
            }
        }

        return Observable.just(
            TVChannelLinkStream(
                tvChannel,
                streamingUrl.map {
                    TVChannel.Url.fromUrl(it.url, type = TVChannelUrlType.STREAM.value)
                }
            )
        )
    }

    fun getStreamUrlByWebUrlAndSrc(
        url: TVChannel.Url,
        tvChannel: TVChannel, isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        tvChannel.tvChannelWebDetailPage = url.url
        return when (url.dataSource) {
            TVChannelURLSrc.VTV.value -> {
                vtvDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
            }

            TVChannelURLSrc.HY.value -> {
                hyDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
            }

            TVChannelURLSrc.SCTV.value -> {
                sctvDataSource.getTvLinkFromDetail(tvChannel, isBackup)
            }

            TVChannelURLSrc.V.value -> {
                vDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
            }

            else -> {
                vDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
            }
        }.onErrorResumeNext {
            Observable.just(
                TVChannelLinkStream(
                    tvChannel,
                    emptyList()
                )
            )
        }
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
        V("vieon"), SCTV("sctv"), HY("hy"), VTV("vtv")
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
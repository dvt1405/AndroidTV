package com.kt.apps.core.tv.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logPlayByDeeplinkTV
import com.kt.apps.core.logging.logStreamingTV
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class BaseTVChannelViewModel constructor(
    private val interactors: TVChannelInteractors,
) : BaseViewModel() {

    @Inject
    lateinit var actionLogger: IActionLogger

    private var _lastWatchedChannel: TVChannelLinkStream? = null
    val lastWatchedChannel: TVChannelLinkStream?
        get() = _lastWatchedChannel

    open val _listTvChannelLiveData by lazy {
        MutableLiveData<DataState<List<TVChannel>>>()
    }

    val tvChannelLiveData: LiveData<DataState<List<TVChannel>>>
        get() = _listTvChannelLiveData


    private val tvChannelStreamingRetryCount: MutableMap<String, Int> by lazy {
        mutableMapOf()
    }

    open fun getListTVChannel(forceRefresh: Boolean, sourceFrom: TVDataSourceFrom = TVDataSourceFrom.MAIN_SOURCE) {
        if (!forceRefresh && interactors.getListChannel.cacheData != null) {
            Logger.d(this, "ListChannel", "Get from cache")
            _listTvChannelLiveData.postValue(DataState.Success(interactors.getListChannel.cacheData!!))
            return
        }
        val finalList = mutableListOf<TVChannel>()
        _listTvChannelLiveData.postValue(DataState.Loading())

        add(
            interactors.getListChannel(forceRefresh, sourceFrom)
                .subscribe({
                    Logger.d(this, message = "Response data: ${Gson().toJson(it)}")
                    finalList.addAll(it)
                }, {
                    Logger.e(this, exception = it)
                    _listTvChannelLiveData.postValue(DataState.Error(it))
                }, {
                    _listTvChannelLiveData.postValue(DataState.Success(finalList))
                    onFetchTVListSuccess(finalList)
                })
        )
    }

    private var lastTVStreamLinkTask: Disposable? = null
    val _tvWithLinkStreamLiveData by lazy { MutableLiveData<DataState<TVChannelLinkStream>>() }
    val tvWithLinkStreamLiveData: LiveData<DataState<TVChannelLinkStream>>
        get() = _tvWithLinkStreamLiveData

    fun getLinkStreamForChannel(tvDetail: TVChannel, isBackup: Boolean = false) {
        _tvWithLinkStreamLiveData.postValue(DataState.Loading())
        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }
        markLastWatchedChannel(tvDetail)
        Logger.d(this, message = "getLinkStreamForChannel: ${Gson().toJson(tvDetail)}")
        var tvChannelStreamLink: TVChannelLinkStream? = null
        lastTVStreamLinkTask = interactors.getChannelLinkStream(tvDetail, isBackup)
            .subscribe({
                tvChannelStreamLink = if (tvChannelStreamLink == null) {
                    it
                } else {
                    val newLinkStream = tvChannelStreamLink!!.linkStream.toMutableList()
                    newLinkStream.addAll(it.linkStream)
                    TVChannelLinkStream(it.channel, newLinkStream)
                }.also {
                    markLastWatchedChannel(it)
                }
            }, {
                Logger.e(this, exception = it)
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
            }, {
                if (tvChannelStreamLink != null && tvChannelStreamLink!!.linkStream.isNotEmpty()) {
                    Logger.d(this, message = Gson().toJson(tvChannelStreamLink))
                    markLastWatchedChannel(tvChannelStreamLink)
                    enqueueInsertWatchNextTVChannel(tvChannelStreamLink!!.channel)
                    _tvWithLinkStreamLiveData.postValue(DataState.Success(tvChannelStreamLink!!))
                    actionLogger.logStreamingTV(tvChannelStreamLink!!.channel.tvChannelName)
                    Logger.d(this, message = "onComplete")
                } else {
                    _tvWithLinkStreamLiveData.postValue(
                        DataState.Error(
                            Throwable(
                                "Kênh ${lastWatchedChannel?.channel?.tvChannelName} " +
                                        "hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí"
                            )
                        )
                    )
                }
            })

        add(lastTVStreamLinkTask!!)
    }

    fun cancelCurrentGetStreamLinkTask() {
        lastTVStreamLinkTask?.let {
            compositeDisposable.remove(it)
            it.dispose()
        }
    }

    fun getLinkStreamById(channelId: String) {
        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }

        lastTVStreamLinkTask = interactors.getChannelLinkStreamById(channelId)
            .subscribe({
                markLastWatchedChannel(it)
                loadProgramForChannel(it.channel)
                enqueueInsertWatchNextTVChannel(it.channel)
                _tvWithLinkStreamLiveData.postValue(DataState.Success(it))
                Logger.d(
                    this, message = "play by deeplink result: {" +
                            "channelId: $channelId, " +
                            "channel: $it" +
                            "}"
                )
                actionLogger.logStreamingTV(it.channel.tvChannelName)
            }, {
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
                Logger.e(this, exception = it)
            })
        add(lastTVStreamLinkTask!!)
    }

    fun playTvByDeepLinks(uri: Uri) {
        val lastPath = uri.pathSegments.last() ?: return
        Logger.d(
            this, message = "play by deeplink: {" +
                    "uri: $uri" +
                    "}"
        )

        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }
        _tvWithLinkStreamLiveData.postValue(DataState.Loading())
        lastTVStreamLinkTask = interactors.getChannelLinkStreamById(lastPath)
            .subscribe({
                markLastWatchedChannel(it)
                loadProgramForChannel(it.channel)
                enqueueInsertWatchNextTVChannel(it.channel)
                _tvWithLinkStreamLiveData.postValue(DataState.Success(it))
                Logger.d(
                    this, message = "play by deeplink result: {" +
                            "uri: $uri, " +
                            "channel: $it" +
                            "}"
                )

                actionLogger.logPlayByDeeplinkTV(
                    uri,
                    it.channel.tvChannelName
                )
                actionLogger.logStreamingTV(it.channel.tvChannelName)
            }, {
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
                Logger.e(this, exception = it)
            })
        add(lastTVStreamLinkTask!!)
    }

    private val _listProgramForChannel by lazy {
        MutableLiveData<DataState<List<TVScheduler.Programme>>>()
    }
    val listProgramForChannel: LiveData<DataState<List<TVScheduler.Programme>>>
        get() = _listProgramForChannel

    fun getListProgramForChannel(tvChannel: TVChannel) {
        add(
            interactors.getListProgrammeForChannel(tvChannel.toChannelDto())
                .map {
                    it.filter {
                        it.channel.remoAllSpecialCharsAndPrefix() == tvChannel.channelIdWithoutSpecialChars
                                && it.isToday()
                    }
                }
                .subscribe({
                    _listProgramForChannel.postValue(DataState.Success(it))
                }, {
                    _listProgramForChannel.postValue(DataState.Error(it))
                })
        )
    }

    fun markLastWatchedChannel(tvChannel: TVChannelLinkStream?) {
        _lastWatchedChannel = tvChannel
    }

    fun markLastWatchedChannel(tvChannel: TVChannel) {
        _lastWatchedChannel = TVChannelLinkStream(
            tvChannel,
            listOf()
        )
    }

    fun retryGetLastWatchedChannel() {
        _lastWatchedChannel?.let {
            val currentRetryCount = tvChannelStreamingRetryCount[it.channel.channelId] ?: 0
            if (currentRetryCount > 2) {
                tvChannelStreamingRetryCount[it.channel.channelId] = 0
                _tvWithLinkStreamLiveData.postValue(
                    DataState.Error(
                        Throwable(
                            "Kênh ${it.channel.tvChannelName} " +
                                    "hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí"
                        )
                    )
                )
            } else if (currentRetryCount > 1) {
                tvChannelStreamingRetryCount[it.channel.channelId] = currentRetryCount + 1
                getLinkStreamForChannel(it.channel, true)
            } else {
                tvChannelStreamingRetryCount[it.channel.channelId] = currentRetryCount + 1
                getLinkStreamForChannel(it.channel, true)
            }
        }
    }

    private val _programmeForChannelLiveData by lazy {
        MutableLiveData<DataState<TVScheduler.Programme>>()
    }

    val programmeForChannelLiveData: LiveData<DataState<TVScheduler.Programme>>
        get() = _programmeForChannelLiveData

    var lastGetProgramme: Long = 0L
        private set

    fun loadProgramForChannel(channel: TVChannel, silentUpdate: Boolean = false) {
        add(
            interactors.getCurrentProgrammeForChannel.invoke(channel.channelId)
                .filter {
                    it.channel.remoAllSpecialCharsAndPrefix() == lastWatchedChannel
                        ?.channel
                        ?.channelIdWithoutSpecialChars
                }
                .subscribe({
                    lastGetProgramme = System.currentTimeMillis()

                    if (silentUpdate) {
                        _programmeForChannelLiveData.postValue(DataState.Update(it))
                    } else {
                        _programmeForChannelLiveData.postValue(DataState.Success(it))
                    }
                }, {
                    if (silentUpdate) {
                        _programmeForChannelLiveData.postValue(
                            DataState.Update(channel.toDefaultProgramme())
                        )
                    } else {
                        _programmeForChannelLiveData.postValue(
                            DataState.Success(channel.toDefaultProgramme())
                        )
                    }

                })
        )
    }

    private fun TVChannel.toDefaultProgramme() = TVScheduler.Programme(
        channel = this.channelId
            .removeAllSpecialChars()
            .removePrefix("viechannel"),
        title = "",
        description = try {
            TVChannelGroup.valueOf(this.tvGroup).value
        } catch (e: Exception) {
            this.tvGroup
        },
    )

    fun String.remoAllSpecialCharsAndPrefix() = removeAllSpecialChars()
        .removePrefix("viechannel")
        .removeSuffix("hd")


    fun clearCurrentPlayingChannelState() {
        _lastWatchedChannel = null
        _tvWithLinkStreamLiveData.postValue(DataState.None())
    }

    open fun enqueueInsertWatchNextTVChannel(tvChannel: TVChannel) {}

    open fun onFetchTVListSuccess(listChannel: List<TVChannel>) {

    }

    init {
        instance++
    }

    companion object {
        private var instance = 0
    }
}
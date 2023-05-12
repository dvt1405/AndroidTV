package com.kt.apps.core.tv.viewmodels

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logPlayByDeeplinkTV
import com.kt.apps.core.logging.logStreamingTV
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import io.reactivex.rxjava3.disposables.Disposable
import javax.inject.Inject

open class BaseTVChannelViewModel constructor(
    private val interactors: TVChannelInteractors,
) : BaseViewModel() {

    @Inject
    lateinit var actionLogger: IActionLogger

    private var _lastWatchedChannel: TVChannelLinkStream? = null
    val lastWatchedChannel: TVChannelLinkStream?
        get() = _lastWatchedChannel

    private val _listTvChannelLiveData by lazy {
        MutableLiveData<DataState<List<TVChannel>>>()
    }

    val tvChannelLiveData: LiveData<DataState<List<TVChannel>>>
        get() = _listTvChannelLiveData


    private val tvChannelStreamingRetryCount: MutableMap<String, Int> by lazy {
        mutableMapOf()
    }

    fun getListTVChannel(forceRefresh: Boolean, sourceFrom: TVDataSourceFrom = TVDataSourceFrom.MAIN_SOURCE) {
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
        _lastWatchedChannel = TVChannelLinkStream(
            tvDetail,
            listOf()
        )
        lastTVStreamLinkTask = interactors.getChannelLinkStream(tvDetail, isBackup)
            .subscribe({
                Logger.d(this, message = Gson().toJson(it))
                markLastWatchedChannel(it)
                enqueueInsertWatchNextTVChannel(it.channel)
                _tvWithLinkStreamLiveData.postValue(DataState.Success(it))
                actionLogger.logStreamingTV(it.channel.tvChannelName)
            }, {
                Logger.e(this, exception = it)
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
            })

        add(lastTVStreamLinkTask!!)
    }

    fun playTvByDeepLinks(uri: Uri) {
        !(uri.host?.contentEquals(Constants.DEEPLINK_HOST) ?: return)
        val lastPath = uri.pathSegments.last() ?: return
        Logger.d(
            this, message = "play by deeplink: {" +
                    "uri: $uri" +
                    "}"
        )

        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }

        lastTVStreamLinkTask = interactors.getChannelLinkStreamById(lastPath)
            .subscribe({
                markLastWatchedChannel(it)
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

    fun markLastWatchedChannel(tvChannel: TVChannelLinkStream?) {
        _lastWatchedChannel = tvChannel
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
            } else {
                tvChannelStreamingRetryCount[it.channel.channelId] = currentRetryCount + 1
                it.channel.urls.size > 2
                getLinkStreamForChannel(it.channel)
            }
        }
    }

    fun clearCurrentPlayingChannelState() {
        _lastWatchedChannel = null
        _tvWithLinkStreamLiveData.postValue(DataState.None())
    }

    open fun enqueueInsertWatchNextTVChannel(tvChannel: TVChannel) {}

    open fun onFetchTVListSuccess(listChannel: List<TVChannel>) {

    }

    init {
        instance++
        Logger.d(this, message = "TVChannelViewModel instance count: $instance")
    }

    companion object {
        private var instance = 0
    }
}
package com.kt.apps.media.mobile.ui.fragments.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.WorkManager
import com.kt.apps.core.Constants
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.viewmodels.BaseTVChannelViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.core.utils.isShortLink
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.isNetworkAvailable
import com.kt.apps.media.mobile.models.NoNetworkException
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TVChannelViewModel @Inject constructor(
    private val interactors: TVChannelInteractors,
    private val app: App,
    private val workManager: WorkManager
) : BaseTVChannelViewModel(interactors) {
    val wrapperListChannel: LiveData<DataState<Map<String, List<TVChannel>>>> = Transformations.map(
        _listTvChannelLiveData
    ) {
        return@map when (it) {
            is DataState.Success -> {
                DataState.Success(it.data.groupBy {
                    it.tvGroup
                })
            }

            is DataState.None -> DataState.None()

            is DataState.Update -> {
                DataState.Update(it.data.groupBy {
                    it.tvGroup
                })
            }

            is DataState.Loading -> DataState.Loading()

            is DataState.Error -> DataState.Error(it.throwable)

            else -> {
                throw IllegalStateException()
            }
        }
    }

    override fun getListTVChannel(forceRefresh: Boolean, sourceFrom: TVDataSourceFrom) {
        if (app.isNetworkAvailable())
            super.getListTVChannel(forceRefresh, sourceFrom)
        else {
            if (interactors.getListChannel.cacheData != null) {
                Logger.d(this, "ListChannel", "Get from cache")
                _listTvChannelLiveData.postValue(DataState.Success(interactors.getListChannel.cacheData!!))
            } else {
                _listTvChannelLiveData.postValue(DataState.Error(NoNetworkException()))
            }
        }
    }

    fun loadLinkStreamForChannel(tvDetail: TVChannel, isBackup: Boolean = false) {
        if (app.isNetworkAvailable())
            getLinkStreamForChannel(tvDetail, isBackup)
        else
            _tvWithLinkStreamLiveData.postValue(DataState.Error(NoNetworkException()))
    }

    fun playMobileTvByDeepLinks(uri: Uri): Boolean {
        !(uri.host?.contentEquals(Constants.DEEPLINK_HOST) ?: return false)
        val lastPath = uri.pathSegments.last() ?: return false
        _tvWithLinkStreamLiveData.postValue(DataState.Loading())
        super.playTvByDeepLinks(uri)
        return true
    }

    fun getExtensionChannel(tvChannel: ExtensionsChannel) {
        val linkToPlay = tvChannel.tvStreamLink
        if (linkToPlay.isShortLink()) {
            compositeDisposable.add(
                Observable.just(linkToPlay.expandUrl())
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        _tvWithLinkStreamLiveData.postValue(
                            DataState.Success(
                                TVChannelLinkStream(
                                    TVChannel.fromChannelExtensions(tvChannel),
                                    arrayListOf(it)
                                )
                            )
                        )
                    }
            )
        } else {
            _tvWithLinkStreamLiveData.postValue(DataState.Loading())
            compositeDisposable.add(
                Observable.just(linkToPlay)
                    .delay(500, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        _tvWithLinkStreamLiveData.postValue(
                            DataState.Success(
                                TVChannelLinkStream(
                                    TVChannel.fromChannelExtensions(tvChannel),
                                    arrayListOf(linkToPlay)
                                )
                            )
                        )
                    }
            )
        }
    }

    companion object {
        private var instance = 0
    }
}
package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

open class BasePlaybackInteractor(private val provider: ViewModelProvider, private val coroutineScope: LifecycleCoroutineScope) {
    private val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val playbackState:  Flow<PlaybackState>
        get() = playbackViewModel.displayState

    val state
        get() = playbackViewModel.stateEvents

    suspend fun playbackError(error: PlaybackThrowable) {
        playbackViewModel.playbackError(error)
    }

}

class TVPlaybackInteractor(
    provider: ViewModelProvider,
    coroutineScope: LifecycleCoroutineScope
) :
    BasePlaybackInteractor(provider, coroutineScope) {
    private val tvChannelViewModels by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val tvChannelList by lazy {
        tvChannelViewModels.tvChannelLiveData
            .asFlow("tvplayback")
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val channelElementList by lazy {
        tvChannelList.mapLatest {
            it.map { channel ->
                ChannelElement.TVChannelElement(channel)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }
}

package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import kotlinx.coroutines.flow.Flow

open class BasePlaybackControlViewModel(private val provider: ViewModelProvider) {
    private val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val playbackState:  Flow<PlaybackState>
        get() = playbackViewModel.displayState

    val loadEvents
        get() = playbackViewModel.loadEvents

    val streamLinkEvents
        get() = playbackViewModel.streamLinkEvents

    val errorEvents
        get() = playbackViewModel.errorEvents

    suspend fun playbackError(error: PlaybackThrowable) {
        playbackViewModel.playbackError(error)
    }

}

class TVPlaybackControlViewModel(provider: ViewModelProvider) :
    BasePlaybackControlViewModel(provider) {
    private val tvChannelViewModels by lazy {
        provider[TVChannelViewModel::class.java]
    }

}
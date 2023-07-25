package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.Stack

open class BasePlaybackControlViewModel(private val provider: ViewModelProvider, private val coroutineScope: LifecycleCoroutineScope) {
    private val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val playbackState:  Flow<PlaybackState>
        get() = playbackViewModel.displayState

    private val _state = MutableSharedFlow<PlaybackViewModel.State>()
    val state
        get() = _state.asSharedFlow()

    init {
        playbackViewModel.stateEvents
            .onEach { _state.emit(it) }
            .launchIn(coroutineScope)
        val last = playbackViewModel.stateEvents.replayCache.last()
        if (last is PlaybackViewModel.State.LOADING) {
            MainScope().launch {
                _state.emit(last)
            }
        }
    }

    suspend fun playbackError(error: PlaybackThrowable) {
        playbackViewModel.playbackError(error)
    }

}

class TVPlaybackControlViewModel(
    provider: ViewModelProvider,
    coroutineScope: LifecycleCoroutineScope
) :
    BasePlaybackControlViewModel(provider, coroutineScope) {
    private val tvChannelViewModels by lazy {
        provider[TVChannelViewModel::class.java]
    }

}

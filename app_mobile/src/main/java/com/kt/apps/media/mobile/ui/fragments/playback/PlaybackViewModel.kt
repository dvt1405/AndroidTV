package com.kt.apps.media.mobile.ui.fragments.playback

import android.util.Log
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    sealed class State {
        object IDLE: State()
        data class LOADING(val data: PrepareStreamLinkData): State()
        data class  PLAYING(val data: StreamLinkData): State()
        data class ERROR(val error: Throwable?): State()
    }

    @Inject
    lateinit var actionLogger: IActionLogger

    private var _stateEvents = MutableSharedFlow<State>(replay = 1)
    val stateEvents
        get() = _stateEvents


    private val _displayState = MutableStateFlow(PlaybackState.Fullscreen)
    val displayState: StateFlow<PlaybackState> = _displayState

    suspend fun changeProcessState(state: State) {
        _stateEvents.emit(state)
    }

    suspend fun playbackError(error: PlaybackThrowable) {
        _stateEvents.emit(State.ERROR(error))
    }

    fun changeDisplayState(newMode: PlaybackState) {
        _displayState.value = newMode
    }
}
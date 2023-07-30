package com.kt.apps.media.mobile.ui.fragments.playback

import android.util.Log
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
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

    private var _stateEvents = MutableSharedFlow<State>()

    val stateEvents
        get() = _stateEvents

    suspend fun changeProcessState(state: State) {
        _stateEvents.emit(state)
    }

    suspend fun playbackError(error: PlaybackThrowable) {
        _stateEvents.emit(State.ERROR(error))
    }
}
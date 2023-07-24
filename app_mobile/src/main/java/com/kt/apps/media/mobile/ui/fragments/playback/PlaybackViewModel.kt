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

    private var _loadEvent = MutableStateFlow<State.LOADING?>(null)
    val loadEvents: Flow<State.LOADING>
        get() = _loadEvent.mapNotNull { it }

    private var _streamLinkData = MutableStateFlow<State.PLAYING?>(null)
    val streamLinkEvents: Flow<State.PLAYING>
        get() = _streamLinkData.mapNotNull { it }

    private var _errorData = MutableSharedFlow<State.ERROR>(replay = 0)
    val errorEvents: Flow<State.ERROR>
        get() = _errorData

    private val _displayState = MutableStateFlow(PlaybackState.Fullscreen)
    val displayState: StateFlow<PlaybackState> = _displayState


//    val playerListener: Player.Listener = object : Player.Listener {
//        override fun onVideoSizeChanged(videoSize: VideoSize) {
//            videoSizeStateLiveData.postValue(videoSize)
//        }
//
//        override fun onPlaybackStateChanged(playbackState: Int) {
//            super.onPlaybackStateChanged(playbackState)
//            Log.d(TAG, "onPlaybackStateChanged: $playbackState")
//            _state. = when(playbackState) {
//                Player.STATE_READY -> State.PLAYING
//                Player.STATE_BUFFERING -> State.LOADING
//                Player.STATE_ENDED -> State.FINISHED(null)
//                Player.STATE_IDLE -> State.IDLE
//                else -> _state.value
//            }
//        }
//        override fun onPlayerError(error: PlaybackException) {
//            super.onPlayerError(error)
//            _state.value  = State.FINISHED(PlaybackFailException(error))
//        }
//    }

    suspend fun changeProcessState(state: State) {
        when(state) {
            is State.LOADING -> _loadEvent.emit(state)
            is State.PLAYING -> _streamLinkData.emit(state)
            else -> { }
        }
    }

    suspend fun playbackError(error: PlaybackThrowable) {
//        _state.emit(State.ERROR(error))
        _errorData.emit(State.ERROR(error))
    }

    fun changeDisplayState(newMode: PlaybackState) {
        _displayState.value = newMode
    }
}
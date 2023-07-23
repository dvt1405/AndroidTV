package com.kt.apps.media.mobile.ui.fragments.channels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackFailException
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.viewmodels.StreamLinkData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    sealed class State {
        object IDLE: State()
        object LOADING: State()
        object  PLAYING: State()
        data class FINISHED(val error: Throwable?): State()
    }

    @Inject
    lateinit var actionLogger: IActionLogger

    val videoSizeStateLiveData: MutableLiveData<VideoSize?> = MutableLiveData(null)

    private val _state = MutableStateFlow<State>(State.IDLE)
    val state: StateFlow<State> = _state

    private val _displayState = MutableStateFlow(PlaybackState.Fullscreen)
    val displayState: StateFlow<PlaybackState> = _displayState

    private val _streamData = MutableStateFlow<StreamLinkData?>(null)
    val streamLinkData: StateFlow<StreamLinkData?>
        get() = _streamData


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
        Log.d(TAG, "Ann changeProcessState: $state ${_state.value}")
        _state.emit(state)
    }

    fun changeDisplayState(newMode: PlaybackState) {
        _displayState.value = newMode
    }

    suspend fun startStream(data: StreamLinkData) {
        _streamData.emit(data)
    }

    suspend fun stopStream() {
        _streamData.emit(null)
    }
}
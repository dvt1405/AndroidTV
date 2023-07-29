package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class UIControlViewModel @Inject constructor(): BaseViewModel() {
    private var _playerState = MutableStateFlow(PlaybackState.Invisible)
    val playerState
        get() = _playerState.asStateFlow()

    private var _openPlayback = MutableSharedFlow<PrepareStreamLinkData>()
    val openPlayback
        get() = _openPlayback.asSharedFlow()

    suspend fun openPlayback(data: PrepareStreamLinkData) {
        _openPlayback.emit(data)
    }

    fun changePlayerState(state: PlaybackState) {
        _playerState.value = state
    }

}
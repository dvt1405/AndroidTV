package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.models.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import javax.inject.Inject

class UIControlViewModel @Inject constructor(): BaseViewModel() {
    private var _playerState = MutableStateFlow(PlaybackState.Invisible)
    val playerState
        get() = _playerState.asStateFlow()

    private var _openPlayback = MutableSharedFlow<PrepareStreamLinkData>()
    val openPlayback
        get() = _openPlayback.asSharedFlow()

    private var _isInPIPMode = MutableStateFlow(false)
    val isInPIPMode
        get() = _isInPIPMode.asStateFlow()

    private var _addSourceState: MutableStateFlow<AddSourceState> = MutableStateFlow(AddSourceState.IDLE)
    val addSourceState = _addSourceState.asStateFlow()

    private var _openSearchEvent: MutableSharedFlow<Unit> = MutableSharedFlow()
    val openSearchEvent = _openSearchEvent.asSharedFlow()

    private var _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    suspend fun openPlayback(data: PrepareStreamLinkData) {
        _openPlayback.emit(data)
    }

    suspend fun changeAddSourceState(data: AddSourceState) {
        _addSourceState.emit(data)
    }
    fun changePlayerState(state: PlaybackState) {
        _playerState.value = state
    }

    fun changePIPMode(isEnable: Boolean) {
       _isInPIPMode.value = isEnable
    }

    suspend fun openSearch(query: String) {
        _openSearchEvent.emit(Unit)
        _searchQuery.emit(query)
    }

}
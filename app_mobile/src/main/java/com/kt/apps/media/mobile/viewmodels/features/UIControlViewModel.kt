package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class UIControlViewModel @Inject constructor(): BaseViewModel() {
    private var _openPlayback = MutableSharedFlow<PrepareStreamLinkData>()
    val openPlayback
        get() = _openPlayback.asSharedFlow()

    suspend fun openPlayback(data: PrepareStreamLinkData) {
        _openPlayback.emit(data)
    }
}
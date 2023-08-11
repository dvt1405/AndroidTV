package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.media.mobile.models.PrepareStreamLinkData

interface IUIControl {
    val uiControlViewModel: UIControlViewModel
}

suspend fun IUIControl.openPlayback(data: PrepareStreamLinkData) {
    uiControlViewModel.openPlayback(data)
}
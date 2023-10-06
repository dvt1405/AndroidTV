package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel

interface IUIControl {
    val uiControlViewModel: UIControlViewModel
    val playbackViewModel: PlaybackViewModel
}

suspend fun IUIControl.openPlayback(data: PrepareStreamLinkData) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    uiControlViewModel.openPlayback(data)
}
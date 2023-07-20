package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.asSuccessFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class PlaybackControlViewModel(private val provider: ViewModelProvider) {
    private val tvChannelViewModels by lazy {
        provider[TVChannelViewModel::class.java]
    }

    private val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val streamData: Flow<StreamLinkData>
        get() = playbackViewModel.streamLinkData.mapNotNull { it }
}
package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.viewmodels.features.IFetchRadioChannel
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

open class BasePlaybackInteractor(
    private val provider: ViewModelProvider,
    private val coroutineScope: LifecycleCoroutineScope
): IUIControl {
    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val playbackState: Flow<PlaybackState>
        get() = uiControlViewModel.playerState

    val state
        get() = playbackViewModel.stateEvents

    suspend fun playbackError(error: PlaybackThrowable) {
        playbackViewModel.playbackError(error)
    }



}

class TVPlaybackInteractor(
    provider: ViewModelProvider,
    coroutineScope: LifecycleCoroutineScope
) :
    BasePlaybackInteractor(provider, coroutineScope), IFetchTVChannelControl {
    override val tvChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val tvChannelList by lazy {
        tvChannelViewModel.tvChannelLiveData
            .asFlow("tvplayback")
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val channelElementList by lazy {
        tvChannelList.mapLatest {
            it.map { channel ->
                ChannelElement.TVChannelElement(channel)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }
}

class RadioPlaybackInteractor(
    provider: ViewModelProvider,
    coroutineScope: LifecycleCoroutineScope
) :
    BasePlaybackInteractor(provider, coroutineScope), IFetchRadioChannel {
    override val tvChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val radioChannelList by lazy {
        tvChannelViewModel.tvChannelLiveData
            .asFlow("tvplayback")
            .map {
                it.filter { channel -> channel.isRadio } .map { channel ->
                    ChannelElement.TVChannelElement(channel)
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }
}
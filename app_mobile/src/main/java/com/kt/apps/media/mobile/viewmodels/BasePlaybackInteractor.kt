package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.asUpdateFlow
import com.kt.apps.media.mobile.viewmodels.features.IFavoriteControl
import com.kt.apps.media.mobile.viewmodels.features.IFetchRadioChannel
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

open class BasePlaybackInteractor(
    private val provider: ViewModelProvider,
    private val coroutineScope: LifecycleCoroutineScope
): IUIControl, IFavoriteControl {
    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    override val favoriteViewModel: FavoriteViewModel by lazy {
        provider[FavoriteViewModel::class.java]
    }

    val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val playbackState: StateFlow<PlaybackState> by lazy {
        uiControlViewModel.playerState
    }

    val isInPipMode: StateFlow<Boolean>
        get() = uiControlViewModel.isInPIPMode

    val state
        get() = playbackViewModel.stateEvents

    override val currentPlayingVideo: StateFlow<StreamLinkData?> by lazy {
        state.mapNotNull { (it as? PlaybackViewModel.State.PLAYING)?.data }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    }

    val listFavorite: StateFlow<List<VideoFavoriteDTO>> by lazy {
        favoriteViewModel.listFavoriteLiveData.asUpdateFlow("IFavoriteControl")
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

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
            .asUpdateFlow(TAG)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    val currentProgrammeForChannel by lazy {
        tvChannelViewModel.programmeForChannelLiveData
            .asUpdateFlow(TAG)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
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
            .asUpdateFlow("tvplayback")
            .map {
                it.filter { channel -> channel.isRadio } .map { channel ->
                    ChannelElement.TVChannelElement(channel)
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    val currentProgrammeForChannel by lazy {
        tvChannelViewModel.programmeForChannelLiveData
            .asUpdateFlow(TAG)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }
}
package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

typealias GroupTVChannel = Map<String, List<TVChannel>>
abstract class ChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : BaseViewModel(), IUIControl {

    val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val networkState: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    override val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    abstract val listChannels: Flow<List<TVChannel>>
    abstract val groupTVChannel: Flow<GroupTVChannel>

    val onMinimalPlayer: StateFlow<Boolean> by lazy {
        uiControlViewModel.playerState
            .map { it == PlaybackState.Minimal }
            .stateIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed(), false)
    }

    val onConnectedNetwork: SharedFlow<Unit> by lazy {
        networkState.networkStatus
            .mapNotNull { if(it == NetworkState.Connected) Unit else null }
            .shareIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed())
    }

    suspend fun getListTVChannelAsync(forceRefresh: Boolean) {
        tvChannelViewModel.getListTVChannel(forceRefresh)
        tvChannelViewModel.tvChannelLiveData.await()
    }
}


class TVChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentViewModel(provider, coroutineContext) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>> by lazy {
        tvChannelViewModel.tvChannelKt
            .mapLatest {
                it.filter { channel -> !channel.isRadio }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel> by lazy {
        listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }
    }
}

class RadioChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentViewModel(provider, coroutineContext) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>> by lazy {
        tvChannelViewModel.tvChannelKt.mapLatest {
            it.filter { channel -> channel.isRadio }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel> by lazy {
        listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }
    }
}
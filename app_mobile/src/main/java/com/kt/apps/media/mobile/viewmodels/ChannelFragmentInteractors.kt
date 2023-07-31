package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.features.IFetchRadioChannel
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

typealias GroupTVChannel = Map<String, List<TVChannel>>
abstract class ChannelFragmentInteractors(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : IUIControl {

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus

    abstract val listChannels: Flow<List<TVChannel>>
    abstract val groupTVChannel: Flow<GroupTVChannel>

    val onMinimalPlayer: StateFlow<Boolean> by lazy {
        uiControlViewModel.playerState
            .map { it == PlaybackState.Minimal }
            .stateIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed(), false)
    }
    fun getListTVChannel(forceRefresh: Boolean) {
        tvChannelViewModel.getListTVChannel(forceRefresh)
    }

    suspend fun getListTVChannelAsync(forceRefresh: Boolean) {
        tvChannelViewModel.getListTVChannel(forceRefresh)
        tvChannelViewModel.tvChannelLiveData.await()
        return
    }
}

class TVChannelFragmentInteractors(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentInteractors(provider, coroutineContext) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>> by lazy {
        tvChannelViewModel.tvChannelLiveData.asSuccessFlow(tag = "tvchannel - listchannels")
            .mapLatest {
                it.filter { channel -> !channel.isRadio }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: StateFlow<GroupTVChannel> by lazy {
        listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }.stateIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed(), emptyMap())
    }
}

class RadioChannelFragmentInteractors(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentInteractors(provider, coroutineContext) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>> by lazy {
        tvChannelViewModel.tvChannelLiveData.asSuccessFlow(tag = "radioChannel - listchannels").mapLatest {
            it.filter { channel -> channel.isRadio }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: StateFlow<GroupTVChannel> by lazy {
        listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }
            .stateIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed(), emptyMap())
    }

}
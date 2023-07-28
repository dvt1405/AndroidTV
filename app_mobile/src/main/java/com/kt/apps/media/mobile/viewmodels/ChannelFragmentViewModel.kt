package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.features.IFetchRadioChannel
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

typealias GroupTVChannel = Map<String, List<TVChannel>>
abstract class ChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext) {

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus

    abstract val listChannels: Flow<List<TVChannel>>
    abstract val groupTVChannel: Flow<GroupTVChannel>


    fun getListTVChannel(forceRefresh: Boolean) {
        tvChannelViewModel.getListTVChannel(forceRefresh)
    }

    suspend fun getListTVChannelAsync(forceRefresh: Boolean) {
        tvChannelViewModel.getListTVChannel(forceRefresh)
        tvChannelViewModel.tvChannelLiveData.asFlow("getListTVChannelAsync").first()
        return
    }
}

class TVChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentViewModel(provider, coroutineContext), IFetchTVChannelControl {
    override val listChannels: Flow<List<TVChannel>>
        get() = tvChannelViewModel.tvChannelLiveData.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel>
        get() = listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }
}

class RadioChannelFragmentViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext)
    : ChannelFragmentViewModel(provider, coroutineContext), IFetchRadioChannel {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>>
        get() = tvChannelViewModel.tvChannelLiveData.asFlow().mapLatest {
            it.filter { channel -> channel.isRadio }
        }
    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel>
        get() = listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }

}
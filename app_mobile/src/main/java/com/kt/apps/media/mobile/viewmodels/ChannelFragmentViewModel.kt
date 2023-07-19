package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.groupAndSort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
typealias GroupTVChannel = Map<String, List<TVChannel>>
abstract class ChannelFragmentViewModel(private val provider: ViewModelProvider) {

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    protected val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
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

    fun loadLinkStreamForChannel(channel: TVChannel) {
        tvChannelViewModel.loadLinkStreamForChannel(channel)
    }
}

class TVChannelFragmentViewModel(private val provider: ViewModelProvider): ChannelFragmentViewModel(provider) {
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

class RadioChannelFragmentViewModel(private val provider: ViewModelProvider): ChannelFragmentViewModel(provider){
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
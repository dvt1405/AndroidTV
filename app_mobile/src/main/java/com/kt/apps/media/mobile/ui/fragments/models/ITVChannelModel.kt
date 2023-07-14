package com.kt.apps.media.mobile.ui.fragments.models

import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.groupAndSort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlin.reflect.KClass

interface IChannelsModelAdapter {
    val listChannels: Flow<List<TVChannel>>
    val groupTVChannel: Flow<GroupTVChannel>
}

abstract class ChannelsModelAdapter(val viewModel: TVChannelViewModel): IChannelsModelAdapter {
    fun getListTVChannel(forceRefresh: Boolean, sourceFrom: TVDataSourceFrom = TVDataSourceFrom.MAIN_SOURCE) {
        viewModel.getListTVChannel(forceRefresh, sourceFrom)
    }
}

class TVChannelsModelAdapter(viewModel: TVChannelViewModel): ChannelsModelAdapter(viewModel) {
    override val listChannels: Flow<List<TVChannel>>
        get() = viewModel.tvChannelLiveData.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel>
        get() = listChannels.mapLatest {
            groupAndSort(it).associate { value ->
                value.first to value.second
            }
        }
}

class RadioChannelsModelAdapter(viewModel: TVChannelViewModel): ChannelsModelAdapter(viewModel) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val listChannels: Flow<List<TVChannel>>
        get() = viewModel.tvChannelLiveData.asFlow().mapLatest {
            it.filter { channel -> channel.isRadio }
        }
    @OptIn(ExperimentalCoroutinesApi::class)
    override val groupTVChannel: Flow<GroupTVChannel>
        get() = listChannels.mapLatest {
            groupAndSort(it).associate {value ->
                value.first to value.second
            }
        }

}
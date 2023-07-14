package com.kt.apps.media.mobile.ui.fragments.dashboard.adapter

import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.ui.fragments.models.ChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.RadioChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.RadioPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.TVPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.RadioChannelsFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment

interface IDashboardHelper {
    fun totalFragment(): ChannelFragment
    fun perChannelFragment(filterCategory: String): PerChannelListFragment
    fun wrapViewModel(viewModel: TVChannelViewModel): ChannelsModelAdapter
}

class TVDashboardHelper: IDashboardHelper {
    override fun totalFragment(): ChannelFragment {
        return TVChannelsFragment()
    }

    override fun perChannelFragment(filterCategory: String): PerChannelListFragment {
        return TVPerChannelListFragment.newInstance(filterCategory)
    }

    override fun wrapViewModel(viewModel: TVChannelViewModel): ChannelsModelAdapter {
        return  TVChannelsModelAdapter(viewModel)
    }

}

class RadioDashboardHelper: IDashboardHelper {
    override fun totalFragment(): ChannelFragment {
        return RadioChannelsFragment()
    }

    override fun perChannelFragment(filterCategory: String): PerChannelListFragment {
        return RadioPerChannelListFragment.newInstance(filterCategory)
    }

    override fun wrapViewModel(viewModel: TVChannelViewModel): ChannelsModelAdapter {
        return RadioChannelsModelAdapter(viewModel)
    }
}
package com.kt.apps.media.mobile.ui.fragments.dashboard.adapter

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.RadioPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.TVPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.RadioChannelsFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.RadioChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel
import kotlin.coroutines.CoroutineContext

interface IDashboardHelper {
    fun totalFragment(): ChannelFragment
    fun perChannelFragment(filterCategory: String): PerChannelListFragment
    fun wrapViewModel(viewModel: ViewModelProvider, context: CoroutineContext): ChannelFragmentViewModel
}

class TVDashboardHelper: IDashboardHelper {
    override fun totalFragment(): ChannelFragment {
        return TVChannelsFragment.newInstance()
    }

    override fun perChannelFragment(filterCategory: String): PerChannelListFragment {
        return TVPerChannelListFragment.newInstance(filterCategory)
    }

    override fun wrapViewModel(viewModel: ViewModelProvider, context: CoroutineContext): ChannelFragmentViewModel {
        return  TVChannelFragmentViewModel(viewModel, context)
    }

}

class RadioDashboardHelper: IDashboardHelper {
    override fun totalFragment(): ChannelFragment {
        return RadioChannelsFragment.newInstance()
    }

    override fun perChannelFragment(filterCategory: String): PerChannelListFragment {
        return RadioPerChannelListFragment.newInstance(filterCategory)
    }

    override fun wrapViewModel(viewModel: ViewModelProvider, context: CoroutineContext): ChannelFragmentViewModel {
        return  RadioChannelFragmentViewModel(viewModel, context)
    }
}
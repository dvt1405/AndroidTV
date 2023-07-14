package com.kt.apps.media.mobile.ui.fragments.tv

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.ui.fragments.models.ChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.RadioChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelsModelAdapter

class TVPerChannelListFragment : PerChannelListFragment() {
    private val _modelAdapter: ChannelsModelAdapter by lazy {
        TVChannelsModelAdapter(ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java])
    }
    override val tvViewModel: ChannelsModelAdapter
        get() = _modelAdapter

    companion object {
        fun newInstance(filterCategory: String): PerChannelListFragment {
            return TVPerChannelListFragment().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}

class RadioPerChannelListFragment : PerChannelListFragment() {
    private val _modelAdapter: ChannelsModelAdapter by lazy {
        RadioChannelsModelAdapter(ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java])
    }
    override val tvViewModel: ChannelsModelAdapter
        get() = _modelAdapter

    companion object {
        fun newInstance(filterCategory: String): PerChannelListFragment {
            return RadioPerChannelListFragment().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}
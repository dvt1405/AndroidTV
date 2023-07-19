package com.kt.apps.media.mobile.ui.fragments.tv

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.RadioChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel

class TVPerChannelListFragment : PerChannelListFragment() {
    private val _modelAdapter by lazy {
        TVChannelFragmentViewModel(ViewModelProvider(requireActivity(), factory))
    }
    override val tvViewModel: ChannelFragmentViewModel
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
    private val _modelAdapter by lazy {
        RadioChannelFragmentViewModel(ViewModelProvider(requireActivity(), factory))
    }
    override val tvViewModel: ChannelFragmentViewModel
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
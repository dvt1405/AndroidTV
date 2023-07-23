package com.kt.apps.media.mobile.ui.fragments.tvchannels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel

class TVChannelsFragment: ChannelFragment() {
    private val _viewModel by lazy {
        TVChannelFragmentViewModel(ViewModelProvider(requireActivity(),factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val viewModel: ChannelFragmentViewModel
        get() = _viewModel

}
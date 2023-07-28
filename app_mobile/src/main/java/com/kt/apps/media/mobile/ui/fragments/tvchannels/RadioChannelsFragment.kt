package com.kt.apps.media.mobile.ui.fragments.tvchannels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.RadioChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.features.loadLinkStreamChannel
import kotlinx.coroutines.launch

class RadioChannelsFragment: ChannelFragment() {
    private val _viewModel by lazy {
        RadioChannelFragmentViewModel(ViewModelProvider(requireActivity(),factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val viewModel: ChannelFragmentViewModel
        get() = _viewModel

    override fun onClickItemChannel(channel: TVChannel) {
        lifecycleScope.launch {
            _viewModel.loadLinkStreamChannel(ChannelElement.TVChannelElement(channel))
        }
    }
}
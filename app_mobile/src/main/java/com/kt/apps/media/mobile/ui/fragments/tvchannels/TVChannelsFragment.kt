package com.kt.apps.media.mobile.ui.fragments.tvchannels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.launch

class TVChannelsFragment: ChannelFragment() {
    private val _viewModel by lazy {
        TVChannelFragmentViewModel(ViewModelProvider(requireActivity(),factory), lifecycleScope.coroutineContext)
    }
    override val viewModel: ChannelFragmentViewModel
        get() = _viewModel

    override fun onClickItemChannel(channel: TVChannel) {
        lifecycleScope.launch {
            _viewModel.openPlayback(PrepareStreamLinkData.TV(channel))
        }
    }

    companion object {
        fun newInstance(): TVChannelsFragment {
            val fragment = TVChannelsFragment()
            return fragment
        }
    }
}
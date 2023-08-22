package com.kt.apps.media.mobile.ui.fragments.tvchannels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentInteractors
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentInteractors
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.launch

class TVChannelsFragment: ChannelFragment() {
    private val _viewModel by lazy {
        TVChannelFragmentInteractors(ViewModelProvider(requireActivity(),factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val viewModel: ChannelFragmentInteractors
        get() = _viewModel

    override fun onClickItemChannel(channel: TVChannel) {
        lifecycleScope.launch {
            _viewModel.openPlayback(PrepareStreamLinkData.TV(channel))
        }
    }
}
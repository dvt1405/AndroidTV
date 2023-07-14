package com.kt.apps.media.mobile.ui.fragments.tvchannels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.ui.fragments.models.ChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelsModelAdapter

class TVChannelsFragment: ChannelFragment() {
    private val _modelAdapter: ChannelsModelAdapter by lazy {
        TVChannelsModelAdapter(ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java])
    }
    override val tvChannelViewModel: ChannelsModelAdapter?
        get() = _modelAdapter
}
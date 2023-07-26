package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.TVPlaybackInteractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest


class TVPlaybackFragment : BasePlaybackFragment() {
    private val _playbackViewModel by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

    private val channelListRecyclerView by lazy {
//        binding.exoPlayer.findViewById<ChannelListView>(R.id.channel_list)
        binding.channelList
    }


    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
//        channelListRecyclerView.changeDisplayStyle(ChannelListView.DisplayStyle.HORIZONTAL_LINEAR)
    }
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            _playbackViewModel.channelElementList.collectLatest {
                delay(2000)
                channelListRecyclerView?.reloadAllData(it)
            }
        }
    }

}

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        BasePlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

}
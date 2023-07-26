package com.kt.apps.media.mobile.ui.fragments.playback

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelAdapter
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

    private val channelListRecyclerView: ChannelListView? by lazy {
//        binding.exoPlayer.findViewById(R.id.channel_list)
        binding.channelList
//        null
    }



    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

        channelListRecyclerView?.apply {
            setOnTouchListener { _, motionEvent ->
                if (binding.motionLayout?.onTouchEvent(motionEvent)?.not() == true) {
                    this.onTouchEvent(motionEvent)
                } else {
                    this.onTouchEvent(motionEvent)
                }
            }
        }
    }


    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            _playbackViewModel.channelElementList.collectLatest {
                channelListRecyclerView?.reloadAllData(it)
//                binding.channelListTemp?.reloadAllData(it)
            }
        }

    }



    override fun onDestroy() {
        super.onDestroy()
//        channelListRecyclerView?.reloadAllData(emptyList())
    }

}

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        BasePlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

}
package com.kt.apps.media.mobile.ui.fragments.playback

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.ui.view.ChannelListView

abstract class ChannelPlaybackFragment : BasePlaybackFragment<FragmentPlaybackBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_playback
    override val screenName: String
        get() = "Fragment Playback"
    override val exoPlayer: StyledPlayerView? by lazy {
        binding.exoPlayer
    }
    override val motionLayout: ConstraintLayout? by lazy {
        binding.motionLayout
    }
    override val minimalLayout: View? by lazy {
        binding.minimalLayout
    }

    protected override val minimalProgress by lazy {
        binding.minimalLoading
    }

    protected override val minimalPlayPause by lazy {
        binding.minimalPlayButton
    }
    override val minimalTitleTv: TextView? by lazy {
        binding.minimalTitleTv
    }

    protected override val channelListRecyclerView: ChannelListView? by lazy {
        binding.channelList
    }
    override val exitButton: View? by lazy {
        binding.exitButton
    }

    override fun onRedraw() {
        super.onRedraw()
        channelListRecyclerView?.forceRedraw()
    }
}
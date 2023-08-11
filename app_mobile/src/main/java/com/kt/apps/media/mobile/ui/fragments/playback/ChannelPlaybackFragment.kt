package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.button.MaterialButton
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.ui.view.RowItemChannelAdapter
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.clicks
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    protected override val minimalPlayPause: MaterialButton? by lazy {
        binding.minimalPlayButton
    }
    override val minimalTitleTv: TextView? by lazy {
        binding.minimalTitleTv
    }

    override val exitButton: View? by lazy {
        binding.exitButton
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        minimalPlayPause?.setOnClickListener {
            if (isPlayingState.value) {
                exoPlayer?.player?.pause()
            } else {
                exoPlayer?.player?.play()
            }
        }
    }
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                isPlayingState.collectLatest { isPlaying ->
                    minimalPlayPause?.icon = if (isPlaying) {
                        resources.getDrawable(R.drawable.ic_pause)
                    } else {
                        resources.getDrawable(R.drawable.ic_play)
                    }
                }
            }

        }
    }
}
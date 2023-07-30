package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballPlaybackBinding
import com.kt.apps.media.mobile.ui.fragments.football.list.SubFootballListAdapter
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.matchParentWidth
import com.kt.apps.media.mobile.utils.repeatLaunchsOnLifeCycle
import com.kt.apps.media.mobile.utils.safeLet
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.FootballPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadFootballMatchLinkStream
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class FootballPlaybackFragment: BasePlaybackFragment<FragmentFootballPlaybackBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_football_playback
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

    override val minimalProgress by lazy {
        binding.minimalLoading
    }

    protected override val minimalPlayPause by lazy {
        binding.minimalPlayButton
    }
    override val minimalTitleTv: TextView? by lazy {
        binding.minimalTitleTv
    }

    private val _adapter: SubFootballListAdapter = SubFootballListAdapter()
    protected override val channelListRecyclerView: RecyclerView? by lazy {
        binding.footballListRecyclerView
    }

    override val exitButton: View? by lazy {
        binding.exitButton
    }
    override val playbackViewModel: FootballPlaybackInteractor by lazy {
        FootballPlaybackInteractor(
            ViewModelProvider(requireActivity(), factory),
            viewLifecycleOwner.lifecycleScope
        )
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        channelListRecyclerView?.apply {
            adapter = _adapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = false
            }
            setHasFixedSize(true)
            addItemDecoration(channelItemDecoration)
        }
    }


    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)

        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED,
        listOf ({
            ((arguments?.get(EXTRA_FOOTBALL_MATCH) as? FootballMatch)?.let { flowOf(it) }
                ?: emptyFlow())
                .collectLatest {
                    playbackViewModel.loadFootballMatchLinkStream(it)
                }
        }, {
            playbackViewModel.liveMatches
                .collectLatest {
                    _adapter.onRefresh(it)
                }
        })
        )
    }

    override fun onRedraw() {
        super.onRedraw()
        channelListRecyclerView?.adapter = null
        channelListRecyclerView?.adapter = _adapter
    }

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.footballListRecyclerView) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.INVISIBLE)
                matchParentWidth(list.id)
                matchParentWidth(minimal.id)
                matchParentWidth(exoplayer.id)
                constrainHeight(minimal.id, ConstraintSet.WRAP_CONTENT)
                connect(exoplayer.id, ConstraintSet.BOTTOM, minimal.id, ConstraintSet.TOP)
                alignParent(minimal.id, ConstraintSet.BOTTOM)
                alignParent(exoplayer.id, ConstraintSet.TOP)
            }
        }
    }

    companion object {
        const val screenName: String = "TVPlaybackFragment"
        private const val EXTRA_FOOTBALL_MATCH = "extra:football_match"
        fun newInstance(
            match: FootballMatch
        ) = FootballPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_FOOTBALL_MATCH to match,
            )
        }
    }
}
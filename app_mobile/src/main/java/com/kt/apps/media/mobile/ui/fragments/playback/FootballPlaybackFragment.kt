package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.ui.fragments.football.list.SubFootballListAdapter
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.viewmodels.FootballPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadFootballMatchLinkStream
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class FootballPlaybackFragment: ChannelPlaybackFragment() {
    private val _adapter: SubFootballListAdapter = SubFootballListAdapter()

    override val exitButton: View? by lazy {
        binding.exitButton
    }
    override val interactor: FootballPlaybackInteractor by lazy {
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

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            lifecycleScope.launch(CoroutineExceptionHandler(coroutineError())) {
                ((arguments?.get(EXTRA_FOOTBALL_MATCH) as? FootballMatch)?.let { flowOf(it) }
                    ?: emptyFlow())
                    .collectLatest {
                        interactor.loadFootballMatchLinkStream(it)
                    }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                interactor.liveMatches
                    .collectLatest {
                        _adapter.onRefresh(it)
                    }
            }
        }
    }

    override fun onRedraw() {
        super.onRedraw()
        channelListRecyclerView?.adapter = null
        channelListRecyclerView?.adapter = _adapter
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
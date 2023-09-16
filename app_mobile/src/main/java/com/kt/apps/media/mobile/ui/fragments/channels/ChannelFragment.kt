package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.ActivityIndicator
import com.kt.apps.media.mobile.utils.avoidExceptionLaunch
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.apps.media.mobile.utils.groupAndSort
import com.kt.apps.media.mobile.utils.launchTrack
import com.kt.apps.media.mobile.utils.onRefresh
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentInteractors
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract  class ChannelFragment: BaseMobileFragment<ActivityMainBinding>() {

    override val layoutResId: Int
        get() = R.layout.activity_main
    override val screenName: String
        get() = "Channel screen"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val loadingChannel: ActivityIndicator = ActivityIndicator()

    //Views
    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val mainRecyclerView by lazy {
        binding.mainChannelRecyclerView
    }

    private val skeletonScreen by lazy {
        KunSkeleton.bind(mainRecyclerView)
            .adapter(adapter)
            .itemCount(10)
            .recyclerViewLayoutItem(
                R.layout.item_row_channel_skeleton,
                R.layout.item_channel_skeleton
            )
            .build()
    }

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            onChildItemClickListener = { item, _ ->
                when (item) {
                    is ChannelElement.TVChannelElement -> onClickItemChannel(item.model)
                }
            }
        }
    }

    abstract val viewModel: ChannelFragmentInteractors

    private val playbackViewModel: PlaybackViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
        }
    }
    override fun initView(savedInstanceState: Bundle?) {

        with(binding.mainChannelRecyclerView) {
            adapter = this@ChannelFragment.adapter
            layoutManager = LinearLayoutManager(this@ChannelFragment.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
//            addOnScrollListener(object: OnScrollListener() {
//                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                    super.onScrollStateChanged(recyclerView, newState)
//                    when(newState) {
//                        SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING -> {
//                            GlideApp.with(this@ChannelFragment.requireContext())
//                                .pauseRequests()
//                        }
//                        SCROLL_STATE_IDLE -> {
//                            GlideApp.with(this@ChannelFragment.requireContext())
//                                .resumeRequests()
//                        }
//                    }
//                }
//            })
        }

        with(binding.swipeRefreshLayout) {
            setDistanceToTriggerSync(screenHeight / 3)
        }

    }


    override fun initAction(savedInstanceState: Bundle?) {
        playbackViewModel

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            lifecycleScope.avoidExceptionLaunch {
                merge(
                    flowOf(Unit),
                    binding.swipeRefreshLayout.onRefresh(),
                    viewModel.onConnectedNetwork
                )
                    .collectLatest {
                        performLoadTVChannel()
                    }
            }

            lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->
                swipeRefreshLayout.isRefreshing = false
            }) {
                viewModel.listChannels.collectLatest { tvChannel ->
                    if (tvChannel.isNotEmpty())
                        reloadOriginalSource(tvChannel)
                }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {

            launch {
                loadingChannel.isLoading.collectLatest {
                    swipeRefreshLayout.isRefreshing = it
                    if (it) {
                        skeletonScreen.run {  }
                    } else {
                        skeletonScreen.hide()
                    }
                }
            }

            if (isLandscape) {
                launch {
                    viewModel.onMinimalPlayer.collectLatest {
                        with(mainRecyclerView) {
                            if (it) {
                                setPadding(0, 0, 0, (screenHeight * 0.5).toInt())
                                clipToPadding = false
                            } else {
                                setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.mainChannelRecyclerView.adapter = null
        adapter.onRefresh(emptyList())
        super.onDestroyView()
    }

   private fun performLoadTVChannel() {
       lifecycleScope.launchTrack(loadingChannel, CoroutineExceptionHandler {  _, _ ->
           showErrorDialog(content = getString(R.string.error_happen))
       }) {
           viewModel.getListTVChannelAsync(true)
       }
    }

    override fun onStop() {
        super.onStop()
        mainRecyclerView.clearOnScrollListeners()
    }

    private fun reloadOriginalSource(data: List<TVChannel>) {
        val grouped = groupAndSort(data).map {
            Pair(
                it.first,
                it.second.map { tvChannel -> ChannelElement.TVChannelElement(tvChannel) })
        }
        swipeRefreshLayout.isRefreshing = false
        adapter.onRefresh(grouped)
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }

    abstract fun onClickItemChannel(channel: TVChannel)

    companion object {
        const val TAG: String = "ChannelFragment"
    }
}
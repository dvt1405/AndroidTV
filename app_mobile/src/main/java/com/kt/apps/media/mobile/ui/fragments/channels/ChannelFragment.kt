package com.kt.apps.media.mobile.ui.fragments.channels

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentInteractors
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.set

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

    private val networkStateViewModel: NetworkStateViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[NetworkStateViewModel::class.java]
        }
    }

    private var _cacheMenuItem: MutableMap<String, Int> = mutableMapOf<String, Int>()
    override fun initView(savedInstanceState: Bundle?) {

        with(binding.mainChannelRecyclerView) {
            adapter = this@ChannelFragment.adapter
            layoutManager = LinearLayoutManager(this@ChannelFragment.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
        }

        with(binding.swipeRefreshLayout) {
            setDistanceToTriggerSync(screenHeight / 3)
        }

    }


    override fun initAction(savedInstanceState: Bundle?) {
        playbackViewModel


        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch(CoroutineExceptionHandler { _, _ ->
                showErrorDialog(content = getString(R.string.error_happen))
            }) {
                merge(flowOf(Unit), binding.swipeRefreshLayout.onRefresh())
                    .collectLatest {
                        launch {
                            viewModel.getListTVChannelAsync(true)
                        }.trackActivity(loadingChannel)
                    }
            }

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

            launch(CoroutineExceptionHandler { context, throwable ->
                swipeRefreshLayout.isRefreshing = false
            }) {
                viewModel.listChannels.collectLatest { tvChannel ->
                    delay(500)
                    if (tvChannel.isNotEmpty())
                        reloadOriginalSource(tvChannel)
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

//        skeletonScreen.hide {
//            scrollToPosition(0)
//        }
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }

    abstract fun onClickItemChannel(channel: TVChannel)
}
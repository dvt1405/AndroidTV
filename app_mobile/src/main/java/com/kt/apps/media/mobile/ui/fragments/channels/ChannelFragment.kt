package com.kt.apps.media.mobile.ui.fragments.channels

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.set

abstract  class ChannelFragment: BaseFragment<ActivityMainBinding>() {

    override val layoutResId: Int
        get() = R.layout.activity_main
    override val screenName: String
        get() = "Channel screen"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val isLandscape: Boolean
        get() = resources.getBoolean(R.bool.is_landscape)

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
//                    is ChannelElement.ExtensionChannelElement -> tvChannelViewModel?.getExtensionChannel(
//                        item.model
//                    )
                }
            }
        }
    }

    abstract val viewModel: ChannelFragmentViewModel

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

        skeletonScreen.run()
    }


    override fun initAction(savedInstanceState: Bundle?) {
        playbackViewModel

        with(binding.swipeRefreshLayout) {
            setDistanceToTriggerSync(screenHeight / 3)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            merge(flowOf(Unit), binding.swipeRefreshLayout.onRefresh())
                .collectLatest {
                    launch {
                        viewModel.getListTVChannelAsync(true)
                    }.trackActivity(loadingChannel)
                }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            loadingChannel.isLoading.collectLatest {
                swipeRefreshLayout.isRefreshing = it
                if (it) {
                    skeletonScreen.run {  }
                } else {
                    skeletonScreen.hide()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            launch(CoroutineExceptionHandler { context, throwable ->
                swipeRefreshLayout.isRefreshing = false
            }) {
                viewModel.listChannels.collectLatest { tvChannel ->
                    delay(500)
                    if (tvChannel.isNotEmpty())
                        reloadOriginalSource(tvChannel)
                }
            }
            if (isLandscape)
//                launch {
//                    playbackViewModel?.state?.collectLatest { state ->
//                        with(mainRecyclerView) {
//                            when (state) {
//                                PlaybackViewModel.State.IDLE -> setPadding(0, 0, 0, 0)
//                                is PlaybackViewModel.State.LOADING -> {
//                                    setPadding(0, 0, 0, (screenHeight * 0.4).toInt())
//                                    clipToPadding = false
//                                }
//                                else -> {}
//                            }
//                        }
//                    }
//                }

            launch {
                networkStateViewModel?.networkStatus?.collectLatest {
//                        Toast.makeText(this@ChannelFragment.context, "$it", Toast.LENGTH_LONG).show()
                    if (it == NetworkState.Connected) {
                        if (adapter.itemCount == 0) {
//                                tvChannelViewModel?.getListTVChannel(forceRefresh = true)
                        } else if (skeletonScreen.isRunning) {
                            skeletonScreen.hide()
                        }
                    }
                    if (it == NetworkState.Connected && adapter.itemCount == 0)
                        viewModel.getListTVChannel(forceRefresh = true)
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
//        extensionsViewModel?.perExtensionChannelData?.replayCache?.forEach {
//            appendExtensionSource(it)
//        }
        skeletonScreen.hide {
            scrollToPosition(0)
        }
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }

    private fun showAlertRemoveExtension(sourceName: String) {
        AlertDialog.Builder(context, R.style.AlertDialogTheme).apply {
            setMessage("Bạn có muốn xóa nguồn $sourceName?")
            setCancelable(true)
            setPositiveButton("Có") { dialog, which ->
                deleteExtension(sourceName = sourceName)
                dialog.dismiss()
            }
            setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
            }
        }
            .create()
            .show()

    }

    private fun deleteExtension(sourceName: String) {
//        extensionsViewModel?.deleteExtension(sourceName = sourceName)
        adapter.listItem.filter {
            return@filter (it.second.firstOrNull() as? ChannelElement.ExtensionChannelElement)
                ?.model
                ?.sourceFrom == sourceName
        }.forEach {
            adapter.onDelete(it)
        }
    }

    private fun reloadNavigationBar(extra: List<ExtensionsConfig>) {
        _cacheMenuItem = mutableMapOf()

        val extraSection = extra.map {
            val id = View.generateViewId()
            _cacheMenuItem[it.sourceName] = id
            SectionItemElement.MenuItem(
                displayTitle = it.sourceName,
                id = id,
                icon = resources.getDrawable(R.drawable.round_add_circle_outline_24)
            )
        }
    }

    private fun onClickItemChannel(channel: TVChannel) {
        viewModel.loadLinkStreamForChannel(channel)
    }
}
package com.kt.apps.media.mobile.ui.fragments.channels

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.models.*
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.debounce
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.apps.media.mobile.utils.groupAndSort
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.reflect.KClass

open abstract  class ChannelFragment: BaseFragment<ActivityMainBinding>() {

    override val layoutResId: Int
        get() = R.layout.activity_main
    override val screenName: String
        get() = "Channel screen"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val isLandscape: Boolean
        get() = resources.getBoolean(R.bool.is_landscape)

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

    private val playbackViewModel: PlaybackViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
        }
    }

    abstract val tvChannelViewModel: ChannelsModelAdapter?

    private val networkStateViewModel: NetworkStateViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[NetworkStateViewModel::class.java]
        }
    }

    private val extensionsViewModel: ExtensionsViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[ExtensionsViewModel::class.java]
        }
    }

//    private val listTVChannelObserver: Observer<DataState<List<TVChannel>>> by lazy {
//        Observer { dataState ->
//            when (dataState) {
//                is DataState.Success -> _tvChannelData.value = dataState.data
//                is DataState.Error -> swipeRefreshLayout.isRefreshing = false
//                else -> {}
//            }
//        }
//    }

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
            doOnPreDraw {
                val spanCount = 3.coerceAtLeast((mainRecyclerView.measuredWidth / 220.dpToPx()))
                this@ChannelFragment.adapter.spanCount = spanCount
            }
        }

        skeletonScreen.run()
    }


    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel
        playbackViewModel
        extensionsViewModel?.loadExtensionData()

        with(binding.swipeRefreshLayout) {
            setDistanceToTriggerSync(screenHeight / 3)
            setOnRefreshListener {
                skeletonScreen.run()
                tvChannelViewModel?.getListTVChannel(true)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            launch(CoroutineExceptionHandler { context, throwable ->
                swipeRefreshLayout.isRefreshing = false
            }) {
                tvChannelViewModel?.listChannels?.collectLatest { tvChannel ->
                    delay(500)
                    if (tvChannel.isNotEmpty())
                        reloadOriginalSource(tvChannel)
                }
            }
            if (isLandscape)
                launch {
                    playbackViewModel?.state?.collectLatest { state ->
                        with(mainRecyclerView) {
                            when (state) {
                                PlaybackViewModel.State.IDLE -> setPadding(0, 0, 0, 0)
                                PlaybackViewModel.State.LOADING, PlaybackViewModel.State.LOADING -> {
                                    setPadding(0, 0, 0, (screenHeight * 0.4).toInt())
                                    clipToPadding = false
                                }
                                else -> {}
                            }
                        }
                    }
                }

            launch {
                extensionsViewModel?.perExtensionChannelData?.collect {
                    appendExtensionSource(it)
                }
            }

            launch {
                extensionsViewModel?.extensionsConfigs?.collectLatest {
                    reloadNavigationBar(it)
                }
            }

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
                        tvChannelViewModel?.getListTVChannel(forceRefresh = true)
                }
            }
        }

//        tvChannelViewModel?.getListTVChannel(savedInstanceState != null)
        tvChannelViewModel?.getListTVChannel(savedInstanceState != null)
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
        extensionsViewModel?.perExtensionChannelData?.replayCache?.forEach {
            appendExtensionSource(it)
        }
        skeletonScreen.hide {
            scrollToPosition(0)
        }
    }

    private fun appendExtensionSource(data: Map<ExtensionsConfig, List<ExtensionsChannel>>) {
        data.forEach { entry ->
            val grouped = groupAndSort(entry.value).map {
                Pair(
                    "${it.first} (${entry.key.sourceName})",
                    it.second.map { exChannel -> ChannelElement.ExtensionChannelElement(exChannel) }
                )
            }
            adapter.onAdd(grouped)
        }
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }


    private fun onChangeItem(item: SectionItem): Boolean {

        when (item.id) {
            R.id.radio -> {
                adapter.listItem.indexOfFirst {
                    val channel = it.first
                    (channel == TVChannelGroup.VOV.value || channel == TVChannelGroup.VOH.value)
                }.takeIf { it != -1 }?.run {
                    scrollToPosition(this)
                }
            }
            R.id.tv -> scrollToPosition(0)
            R.id.add_extension -> {
                val dialog = AddExtensionFragment()
                dialog.onSuccess = {
                    it.dismiss()
                    onAddedExtension()
                }
                dialog.show(this@ChannelFragment.parentFragmentManager, AddExtensionFragment.TAG)
                return false
            }
            else -> {
                val title = item.displayTitle
                return (extensionsViewModel?.extensionsConfigs?.value ?: emptyList()).findLast {
                    it.sourceName == title
                }?.run {
                    adapter.listItem.indexOfFirst {
                        val channel = it.second.firstOrNull()
                        (channel as? ChannelElement.ExtensionChannelElement)?.model?.sourceFrom?.equals(
                            title
                        ) == true
                    }.takeIf {
                        it != -1
                    }?.run {
                        scrollToPosition(this)
                    }

                    true
                } ?: false
            }
        }
        return true
    }

    private fun onAddedExtension() {
        showSuccessDialog(
            content = "Thêm nguồn kênh thành công!\r\nKhởi động lại ứng dụng để kiểm tra nguồn kênh"
        )
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
        extensionsViewModel?.deleteExtension(sourceName = sourceName)
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
//        tvChannelViewModel?.loadLinkStreamForChannel(channel)
    }
}
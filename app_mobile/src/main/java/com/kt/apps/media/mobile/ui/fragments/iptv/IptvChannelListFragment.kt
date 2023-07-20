package com.kt.apps.media.mobile.ui.fragments.iptv

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentChannelListBinding
import com.kt.apps.media.mobile.databinding.FragmentFootballListBinding
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.ui.view.ChannelListData
import com.kt.apps.media.mobile.ui.view.childItemClicks
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.IPTVListViewModel
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class IptvChannelListFragment : BaseFragment<FragmentChannelListBinding>(){
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_channel_list

    override val screenName: String
        get() = "IptvChannelListFragment"

    private val filterCategory by lazy {
        requireArguments().getString(PerChannelListFragment.EXTRA_TV_CHANNEL_CATEGORY)!!
    }

    private val viewModels by lazy {
        activity?.let {
            IPTVListViewModel(ViewModelProvider(it, factory), filterCategory)
        }
    }

    private val activityIndicator by lazy { ActivityIndicator() }

    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val viewSwitcher by lazy {
        binding.viewSwitcher
    }

    private val recyclerView by lazy {
        binding.listChannelRecyclerview
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.swipeRefreshLayout?.apply {
            setDistanceToTriggerSync(screenHeight / 3)
        }
//        recyclerView.onChildItemClickListener = {
//            item, position ->
//            if (item is ChannelElement.ExtensionChannelElement) {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    viewModels?.loadIPTV(item.model)
//                }
//            }
//        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            merge(flowOf(Unit), swipeRefreshLayout.onRefresh() ?: emptyFlow()).collectLatest(loadData)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            activityIndicator.isLoading.collectLatest {
                swipeRefreshLayout.isRefreshing = it
                recyclerView.showHideSkeleton(it)
            }
        }

//        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
//            recyclerView.childItemClicks()
//                .filter { it.data is ChannelElement.ExtensionChannelElement }
//                .collectLatest {
//                    viewModels?.loadIPTV((it.data as ChannelElement.ExtensionChannelElement).model)
//                }
//        }
        var previousJob: Job? = null
        recyclerView.childItemClicks()
            .filter { it.data is ChannelElement.ExtensionChannelElement }
            .onEach {
                previousJob?.cancelAndJoin()
                previousJob = viewModels?.loadIPTVJob((it.data as ChannelElement.ExtensionChannelElement).model)
                previousJob?.start()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private val loadData: suspend (Unit) -> Unit = {
        viewModels?.apply {
            try {
                viewSwitcher.showContentView()
                val list = loadDataAsync().trackActivity(activityIndicator).await()
                val grouped = groupAndSort(list).map {
                    ChannelListData(it.first, it.second.map {channel ->
                        ChannelElement.ExtensionChannelElement(channel)
                    })
                }
                binding.listChannelRecyclerview.reloadAllData(grouped)
            } catch (e: Throwable) {
                Log.d(TAG, "loadData: $e ")
                viewSwitcher.showError()
            }
        }
    }

    companion object {
        internal const val EXTRA_TV_CHANNEL_CATEGORY = "extra:tv_channel_category"
        fun newInstance(filterCategory: String): IptvChannelListFragment {
            return IptvChannelListFragment().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}
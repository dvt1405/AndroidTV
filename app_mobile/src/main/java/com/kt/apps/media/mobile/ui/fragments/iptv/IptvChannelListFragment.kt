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
import com.kt.apps.media.mobile.databinding.FragmentFootballListBinding
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.IPTVListViewModel
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

class IptvChannelListFragment : BaseFragment<FragmentFootballListBinding>(){
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_football_list

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

    private val skeletonScreen by lazy {
        binding.mainChannelRecyclerView?.let {
            KunSkeleton.bind(it)
                .adapter(_adapter)
                .itemCount(10)
                .recyclerViewLayoutItem(
                    R.layout.item_row_channel_skeleton,
                    R.layout.item_channel_skeleton
                )
                .build()
        }
    }

    private val _adapter by lazy {
        TVDashboardAdapter()
    }

    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val viewSwitcher by lazy {
        binding.viewSwitcher
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.mainChannelRecyclerView?.apply {
            adapter = _adapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
        }
        binding.swipeRefreshLayout?.apply {
            setDistanceToTriggerSync(screenHeight / 3)
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            merge(flowOf(Unit), swipeRefreshLayout?.onRefresh() ?: emptyFlow()).collectLatest(loadData)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            activityIndicator.isLoading.collectLatest {
                swipeRefreshLayout?.isRefreshing = it
                if (it) {
                    skeletonScreen?.run()
                } else {
                    skeletonScreen?.hide()
                }
            }
        }
    }

    private val loadData: suspend (Unit) -> Unit = {
        viewModels?.apply {
            try {
                viewSwitcher?.showContentView()
                val list = loadDataAsync().trackActivity(activityIndicator).await()
                val grouped = groupAndSort(list).map {
                    Pair(
                        it.first,
                        it.second.map { channel -> ChannelElement.ExtensionChannelElement(channel) }
                    )
                }
                _adapter.onRefresh(grouped)
            } catch (e: Throwable) {
                Log.d(TAG, "loadData: $e ")
                viewSwitcher?.showError()
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
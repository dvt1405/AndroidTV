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
import com.kt.apps.media.mobile.utils.groupAndSort
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.apps.media.mobile.viewmodels.IPTVListViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val _adapter by lazy {
        TVDashboardAdapter()
    }

    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
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
            loadData()
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.d(TAG, "loadData: $throwable")
        }) {
            swipeRefreshLayout?.isRefreshing = true
            val list = groupAndSort(viewModels?.loadData() ?: emptyList()).map {
                Pair(
                    it.first,
                    it.second.map { channel -> ChannelElement.ExtensionChannelElement(channel) }
                )
            }
            _adapter.onRefresh(list)
            swipeRefreshLayout?.isRefreshing = false
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
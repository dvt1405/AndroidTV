package com.kt.apps.media.mobile.ui.fragments.iptv

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentChannelListBinding
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListData
import com.kt.apps.media.mobile.ui.view.childItemClicks
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.IPTVListInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadIPTVJob
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

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
            IPTVListInteractor(ViewModelProvider(it, factory), viewLifecycleOwner.lifecycleScope.coroutineContext, filterCategory)
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
    }

    override fun initAction(savedInstanceState: Bundle?) {
        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                merge(flowOf(Unit), swipeRefreshLayout.onRefresh()).collectLatest(loadData)
            }

            launch {
                activityIndicator.isLoading.collectLatest {
                    swipeRefreshLayout.isRefreshing = it
                    recyclerView.showHideSkeleton(it)
                }
            }

            launch {
                viewModels?.isMinimalPlayback?.collectLatest {
                    val paddingSize: Int = if (it) {
                        (screenHeight * 0.5).toInt()
                    } else {
                        0
                    }
                    binding.listChannelRecyclerview.setPadding(0, 0, 0 , paddingSize)
                }
            }
        }

        recyclerView.childItemClicks()
            .mapNotNull { it.data as? ChannelElement.ExtensionChannelElement }
            .onEach {
                viewModels?.openPlayback(PrepareStreamLinkData.IPTV(it.model, filterCategory))
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private val loadData: suspend (Unit) -> Unit = {
        viewModels?.apply {
            lifecycleScope.launch(CoroutineExceptionHandler { _, e ->
                Log.d(TAG, "loadData: $e ")
                viewSwitcher.showError()
            }) {
                viewSwitcher.showContentView()
                val list = loadDataAsync().await()
                val grouped = groupAndSort(list).map {
                    ChannelListData(it.first, it.second.map {channel ->
                        ChannelElement.ExtensionChannelElement(channel)
                    })
                }
                binding.listChannelRecyclerview.reloadAllData(grouped)
            }
                .trackJob(activityIndicator)
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

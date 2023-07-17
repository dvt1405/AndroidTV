package com.kt.apps.media.mobile.ui.fragments.football.list

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballListBinding
import com.kt.apps.media.mobile.utils.onRefresh
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.apps.media.mobile.viewmodels.MobileFootballViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FootballListFragment : BaseFragment<FragmentFootballListBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_football_list
    override val screenName: String
        get() = "FootballList"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val viewModel: MobileFootballViewModel by lazy {
        MobileFootballViewModel(ViewModelProvider(requireActivity(), factory))
    }

    private val _adapter = FootballListAdapter()

    override fun initView(savedInstanceState: Bundle?) {
        binding.swipeRefreshLayout?.apply {
            setDistanceToTriggerSync(screenHeight / 3)
        }
        binding.mainChannelRecyclerView?.apply {
            adapter = _adapter
            layoutManager = LinearLayoutManager(this@FootballListFragment.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
        }

    }

    override fun initAction(savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            merge(flowOf(Unit), swipeRefreshLayout?.onRefresh() ?: emptyFlow()).collectLatest {
                loadAllMatches()
            }
        }
    }

    private fun loadAllMatches() {
        CoroutineScope(Dispatchers.Main).launch {
            swipeRefreshLayout?.isRefreshing = true
            _adapter.onRefresh(emptyList())
            viewModel.getAllMatches()
            val list = viewModel.groupedMatches
                .combine(viewModel.liveMatches, transform = { groupsMatches, liveMatches ->
                    val baseList = mutableListOf<FootballAdapterType>()
                    if (liveMatches.isNotEmpty()) {
                        baseList.add(
                            FootballAdapterType(
                                Pair(getString(R.string.live_matches), liveMatches),
                                true
                            )
                        )
                    }
                    baseList.addAll(groupsMatches.toList().map {
                        FootballAdapterType(it, false)
                    })
                    baseList
                }).first()
            _adapter.onRefresh(list)
            swipeRefreshLayout?.isRefreshing = false
        }
    }
}
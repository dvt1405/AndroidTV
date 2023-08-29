package com.kt.apps.media.mobile.ui.fragments.football.list

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballListBinding
import com.kt.apps.media.mobile.utils.ActivityIndicator
import com.kt.apps.media.mobile.utils.exceptionHandler
import com.kt.apps.media.mobile.utils.onRefresh
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.apps.media.mobile.utils.trackActivity
import com.kt.apps.media.mobile.viewmodels.FootballListInteractor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class FootballListFragment : BaseFragment<FragmentFootballListBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_football_list
    override val screenName: String
        get() = "FootballList"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val loadingMatches: ActivityIndicator = ActivityIndicator()

    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val interactor: FootballListInteractor by lazy {
        FootballListInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }

    private val _adapter = FootballListAdapter().apply {
        onChildItemClickListener = { item, position ->
            this@FootballListFragment.lifecycleScope.launch {
                interactor.openPlayback(item)
            }
        }
    }

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
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                merge(flowOf(Unit), swipeRefreshLayout.onRefresh()).collectLatest {
                    lifecycleScope.launch(exceptionHandler { _, _ ->
                        showErrorDialog(content = getString(R.string.error_happen))
                    }) {
                        interactor.getAllMatchesAsync()
                    }.trackActivity(loadingMatches)
                }
            }
        }
        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                interactor.groupedMatches
                    .combine(interactor.liveMatches, transform = { groupsMatches, liveMatches ->
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
                    }).collectLatest {
                        _adapter.onRefresh(it)
                    }
            }

            launch {
                loadingMatches.isLoading.collectLatest {
                    swipeRefreshLayout?.isRefreshing = it
                }
            }

            launch {
                interactor.playbackPadding.collectLatest {
                    val paddingSize: Int = if (it) {
                        (screenHeight * 0.5).toInt()
                    } else {
                        0
                    }
                    binding.mainChannelRecyclerView.setPadding(0, 0, 0, paddingSize)
                }
            }
        }
    }
}
package com.kt.apps.media.mobile.ui.fragments.football.list

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballListBinding
import com.kt.apps.media.mobile.viewmodels.MobileFootballViewModel
import com.kt.apps.media.mobile.viewmodels.features.FootballViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FootballListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FootballListFragment : BaseFragment<FragmentFootballListBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_football_list
    override val screenName: String
        get() = "FootballList"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: MobileFootballViewModel by lazy {
        MobileFootballViewModel(ViewModelProvider(requireActivity(), factory))
    }

    private val _adapter = FootballListAdapter()

    override fun initView(savedInstanceState: Bundle?) {
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.groupedMatches
                .combine(viewModel.liveMatches, transform = { groupsMatches, liveMatches ->
                    val baseList = mutableListOf<FootballAdapterType>()
                    if (liveMatches.isNotEmpty()) {
                        baseList.add(FootballAdapterType(
                            Pair(getString(R.string.live_matches), liveMatches),
                            true
                        ))
                    }
                    baseList.addAll(groupsMatches.toList().map {
                        FootballAdapterType(it, false)
                    })
                    baseList
                }).collectLatest {
                    _adapter.onRefresh(it)
                }
        }

        viewModel.getAllMatches()
    }
}
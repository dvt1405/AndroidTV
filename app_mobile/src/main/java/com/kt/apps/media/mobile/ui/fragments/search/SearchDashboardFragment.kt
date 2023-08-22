package com.kt.apps.media.mobile.ui.fragments.search

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentSearchDashboardBinding
import com.kt.apps.media.mobile.databinding.TextviewItemBinding
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.utils.PaddingItemDecoration
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.textChanges
import com.kt.apps.media.mobile.viewmodels.SearchDashboardViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class SearchDashboardFragment : BaseMobileFragment<FragmentSearchDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    override val layoutResId: Int
        get() = R.layout.fragment_search_dashboard
    override val screenName: String
        get() = "SearchDashboardFragment"

    private val viewModel by lazy {
        SearchDashboardViewModel(ViewModelProvider(requireActivity(), factory), requireContext())
    }

    private val historyAdapter by lazy {
        HistoryAdapter().apply {
            onItemRecyclerViewCLickListener = object: OnItemRecyclerViewCLickListener<String> {
                override fun invoke(item: String, position: Int) {
                    binding.searchInputText?.setText(item)
                }
            }
        }
    }


    override fun initView(savedInstanceState: Bundle?) {
        binding.currentSearchContainer?.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            addItemDecoration(PaddingItemDecoration(PaddingItemDecoration.Edge(0, 12, 0, 0)))
        }
    }
    @OptIn(FlowPreview::class)
    override fun initAction(savedInstanceState: Bundle?) {
        Log.d(TAG, "SearchDashboardFragment: initAction")
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                viewModel.searchQueryData
                    .collectLatest {
                        Log.d(TAG, "SearchDashboardFragment: $it")
                        binding.searchInputText.setText(it)
                    }
            }
        }
        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.registerHistorySearchList().collectLatest {
                    historyAdapter.onRefresh(it)
                }
            }



            if (!isLandscape) {
                launch {
                    viewModel.onOpenPlayback
                        .collectLatest {
                            activity?.onBackPressed()
                        }
                }
            }

        }

        binding.searchInputText?.textChanges()
            ?.debounce(2000)
            ?.map { it.toString() }
            ?.filter { it.isNotEmpty() }
            ?.distinctUntilChanged()
            ?.onEach { viewModel.saveHistorySearch(it) }
            ?.launchIn(viewLifecycleOwner.lifecycleScope)

        binding.searchInputText?.textChanges()
            ?.debounce(250)
            ?.map { it.toString() ?: "" }
            ?.distinctUntilChanged()
            ?.onEach(performSearchChange)
            ?.launchIn(viewLifecycleOwner.lifecycleScope)


    }

    private val performSearchChange: (String) -> Unit = {
        if (it.isNotEmpty()) {
            viewModel.performSearch(it)
        } else {
            viewModel.performClearSearch()
        }
    }

    companion object {
        fun newInstance(): SearchDashboardFragment {
            val fragment = SearchDashboardFragment()
            return fragment
        }
    }

}

class HistoryAdapter: BaseAdapter<String, TextviewItemBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.textview_item
    override fun bindItem(
        item: String,
        binding: TextviewItemBinding,
        position: Int,
        holder: BaseViewHolder<String, TextviewItemBinding>
    ) {
        binding.textView.text = item
        binding.textView.setOnClickListener {
            onItemRecyclerViewCLickListener(item, position)
        }
    }

}
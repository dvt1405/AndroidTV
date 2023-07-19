package com.kt.apps.media.mobile.ui.fragments.search

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentSearchDashboardBinding
import com.kt.apps.media.mobile.databinding.LightItemChannelBinding
import com.kt.apps.media.mobile.databinding.TextviewItemBinding
import com.kt.apps.media.mobile.utils.PaddingItemDecoration
import com.kt.apps.media.mobile.utils.onSubmit
import com.kt.apps.media.mobile.utils.submits
import com.kt.apps.media.mobile.viewmodels.SearchDashboardViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.subscribe
import javax.inject.Inject

class SearchDashboardFragment : BaseFragment<FragmentSearchDashboardBinding>() {
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
        HistoryAdapter()
    }


    override fun initView(savedInstanceState: Bundle?) {
        binding.currentSearchContainer?.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            addItemDecoration(PaddingItemDecoration(PaddingItemDecoration.Edge(0, 12, 0, 0)))
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.registerHistorySearchList().collectLatest {
                historyAdapter.onRefresh(it)
            }
        }

        binding.searchInputText?.submits()
            ?.mapNotNull { it }
            ?.onEach { viewModel.performSearch(it.toString()) }
            ?.launchIn(viewLifecycleOwner.lifecycleScope)
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
    }

}
package com.kt.apps.media.mobile.ui.fragments.search

import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
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
import com.kt.apps.media.mobile.utils.showKeyboard
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
        get() = name

    private val viewModel by lazy {
        SearchDashboardViewModel(ViewModelProvider(requireActivity(), factory), requireContext())
    }   

    private val historyAdapter by lazy {
        HistoryAdapter().apply {
            onItemRecyclerViewCLickListener = object: OnItemRecyclerViewCLickListener<String> {
                override fun invoke(item: String, position: Int) {
                    binding.searchInputText.apply {
                        setText(item)
                    }
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

//        binding.
//        binding.backButton?.setOnClickListener {
//            activity?.onBackPressedDispatcher?.onBackPressed()
////            activity?.onBackPressed()
//        }
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
        }

        binding.searchInputText?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.text.toString().trim()
                    .takeIf { it.isNotEmpty() }
                    ?.run {
                        viewModel.saveHistorySearch(this)
                        lifecycleScope.launch {
                            viewModel.performSearch(this@run)
                        }

                    }
            }
            false
        }

        binding.searchInputText?.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus ) {
                binding.searchInputText.text.toString().trim().takeIf { it.isNotEmpty() }
                    ?.run {
                        viewModel.saveHistorySearch(this)
                        lifecycleScope.launch {
                            viewModel.performSearch(this@run)
                        }
                    }
            }
        }

        binding.searchInputText?.textChanges()
            ?.debounce(250)
            ?.map { it.toString().trim() }
            ?.distinctUntilChanged()
            ?.onEach(performSearchChange)
            ?.launchIn(viewLifecycleOwner.lifecycleScope)


    }

    private val performSearchChange: (String) -> Unit = {
        if (it.isNotEmpty()) {
            lifecycleScope.launch {
                viewModel.performSearch(it)
            }
        } else {
            viewModel.performClearSearch()
        }
    }

    companion object {

        const val name = "SearchDashboardFragment"
        fun newInstance(): SearchDashboardFragment {
            val fragment = SearchDashboardFragment()
            return fragment
        }
    }

    fun EditText.filterEmoji() {
        filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            source.filter { Character.getType(it) != Character.SURROGATE.toInt() }
        })
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
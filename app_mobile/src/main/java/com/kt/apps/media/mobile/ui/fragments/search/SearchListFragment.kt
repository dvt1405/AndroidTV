package com.kt.apps.media.mobile.ui.fragments.search

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentSearchListBinding
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListData
import com.kt.apps.media.mobile.ui.view.childItemClicks
import com.kt.apps.media.mobile.viewmodels.SearchListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


class SearchListFragment : BaseFragment<FragmentSearchListBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_search_list
    override val screenName: String
        get() = "SearchListFragment"

    private val viewModel by lazy {
        SearchListViewModel(ViewModelProvider(requireActivity(), factory))
    }

    override fun initView(savedInstanceState: Bundle?) {

    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewModel.searchResult
            .onEach(display)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isProgressing
                        .map { if (it) View.VISIBLE else View.GONE }
                        .collectLatest { binding.progressBarContainer.visibility = it }
                }

                launch {
                    binding.channelList.childItemClicks()
                        .collectLatest {
                            viewModel.openPlayback(it.data)
                        }
                }
            }
        }
    }


    private fun mapToView(map: Map<String, List<SearchForText.SearchResult>>) : List<ChannelListData> {
        return map.toList().map {
            val value = it.second.map {value ->
                when(value) {
                    is SearchForText.SearchResult.ExtensionsChannelWithCategory -> ChannelElement.SearchExtension(value)
                    is SearchForText.SearchResult.History -> ChannelElement.SearchHistory(value)
                    is SearchForText.SearchResult.TV -> ChannelElement.SearchTV(value)
                }
            }
            ChannelListData(it.first, value)
        }
    }

    private fun showEmptyPlaceholder() {
        if(binding.viewSwitcher.currentView.id != R.id.empty_placeholder) {
            binding.viewSwitcher.showNext()
        }
    }

    private fun showRecyclerView() {
        if(binding.viewSwitcher.currentView.id == R.id.empty_placeholder) {
            binding.viewSwitcher.showNext()
        }
    }

    private val display: (Map<String,List<SearchForText.SearchResult>>) -> Unit = {
        val mappedData = mapToView(it)
        if (mappedData.isEmpty()) {
            showEmptyPlaceholder()
        } else {
            showRecyclerView()
        }
        binding.channelList.reloadAllData(mapToView(it))
    }
}


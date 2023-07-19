package com.kt.apps.media.mobile.ui.fragments.search

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentSearchListBinding
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListData
import com.kt.apps.media.mobile.viewmodels.SearchListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    }

    private fun mapToView(map: Map<String, List<SearchForText.SearchResult>>) : List<ChannelListData> {
        return map.toList().map {
            val value = it.second.map {value ->
                when(value) {
                    is SearchForText.SearchResult.ExtensionsChannelWithCategory -> ChannelElement.SearchExtension(value.data)
                    is SearchForText.SearchResult.History -> ChannelElement.SearchHistory(value.data)
                    is SearchForText.SearchResult.TV -> ChannelElement.SearchTV(value.data)
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


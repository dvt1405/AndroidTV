package com.kt.apps.media.mobile.ui.fragments.search

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentSearchListBinding
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
//        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
//            viewModel.searchResult.map { map ->
//                map.mapValues { entry ->
//                    entry.value.map {searchResult ->
//                        when (searchResult) {
//                            is SearchForText.SearchResult.ExtensionsChannelWithCategory -> TODO()
//                            is SearchForText.SearchResult.History -> TODO()
//                            is SearchForText.SearchResult.TV -> TODO()
//                        }
//                    }
//                }
//            }
//        }
        viewModel.searchResult
            .onEach {
                Log.d(TAG, "searchResult: $it")
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    fun mapToView(map: Map<String, List<SearchForText.SearchResult>>) { }
}


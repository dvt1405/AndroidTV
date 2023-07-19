package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class SearchListViewModel(private val provider: ViewModelProvider) {
    private val searchViewModel by lazy {
        provider[SearchViewModels::class.java]
    }

   val searchResult: Flow<Map<String, List<SearchForText.SearchResult>>>
        get() = searchViewModel.searchQueryLiveData
            .asFlow()
            .catch { emit(emptyMap()) }
}
package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asSuccessFlow
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
import kotlinx.coroutines.flow.Flow

class IPTVViewModel(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val searchViewModel: SearchViewModels by lazy {
        provider[SearchViewModels::class.java]
    }
    val extensionConfigs = extensionViewModel.extensionConfigsKt


    suspend fun remove(config: ExtensionsConfig) {
        extensionViewModel.removeExtensionConfig(config)
        searchViewModel.clearSearchList()
    }
}
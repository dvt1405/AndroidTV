package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class IPTVViewModel(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    val extensionConfigs: Flow<List<ExtensionsConfig>>
        get() = extensionViewModel.totalExtensionsConfig.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val extensionsSourceName: Flow<List<String>>
        get() = extensionConfigs.mapLatest {list ->
            list.map { it.sourceName }
        }
}
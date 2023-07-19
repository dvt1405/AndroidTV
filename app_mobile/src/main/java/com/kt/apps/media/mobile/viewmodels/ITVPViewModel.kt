package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

class IPTVViewModel(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    val extensionConfigs: Flow<List<ExtensionsConfig>>
        get() = extensionViewModel.totalExtensionsConfig.asFlow()

    val addExtensionsConfig: Flow<ExtensionsConfig>
        get() = extensionViewModel.addExtensionConfigLiveData.asFlow()
            .onEach { extensionViewModel.loadAllListExtensionsChannelConfig() }
}
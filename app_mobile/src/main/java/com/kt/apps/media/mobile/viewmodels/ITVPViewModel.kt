package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asSuccessFlow
import kotlinx.coroutines.flow.Flow

class IPTVViewModel(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    val extensionConfigs: Flow<List<ExtensionsConfig>> by lazy {
        extensionViewModel.totalExtensionsConfig.asSuccessFlow("IPTVViewModel")
    }

    val addExtensionsConfig: Flow<ExtensionsConfig> by lazy {
        extensionViewModel.addExtensionConfigLiveData.asSuccessFlow(tag = "IPTVViewModel_addExtensionsConfig")
    }

    fun reloadData() {
        extensionViewModel.loadAllListExtensionsChannelConfig(true)
    }

    suspend fun remove(config: ExtensionsConfig) {
        extensionViewModel.removeExtensionConfig(config)
    }
}
package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment.Companion.TAG
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.asSuccessFlow
import com.kt.apps.media.mobile.utils.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class IPTVViewModel(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    val extensionConfigs: Flow<List<ExtensionsConfig>> by lazy {
        extensionViewModel.totalExtensionsConfig.asSuccessFlow("IPTVViewModel")
    }

    val addExtensionsConfig: Flow<ExtensionsConfig>
        get() = extensionViewModel.addExtensionConfigLiveData.asSuccessFlow(tag = "IPTVViewModel_addExtensionsConfig")

    fun reloadData() {
        extensionViewModel.loadAllListExtensionsChannelConfig(true)
    }

    suspend fun remove(config: ExtensionsConfig) {
        extensionViewModel.removeExtensionConfig(config)
    }
}
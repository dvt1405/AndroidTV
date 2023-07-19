package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AddIptvViewModels(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    val addExtensionsConfig
        get() = extensionViewModel.addExtensionConfigLiveData.asFlow()

    suspend fun addIPTVSourceAsync(config: ExtensionsConfig): ExtensionsConfig {
        extensionViewModel.addIPTVSource(config)
        return extensionViewModel.addExtensionConfigLiveData.asFlow().first()
    }
}
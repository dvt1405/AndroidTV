package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddIptvViewModels(private val provider: ViewModelProvider) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    fun addIPTVSourceAsync(config: ExtensionsConfig) {
        CoroutineScope(Dispatchers.Main).launch {
            uiControlViewModel.changeAddSourceState(AddSourceState.StartLoad(config))
            extensionViewModel.addIPTVSource(config)
            try {
                val result = extensionViewModel.addExtensionConfigLiveData.await()
                uiControlViewModel.changeAddSourceState(AddSourceState.Success(config))
            } catch (t: Throwable) {
                uiControlViewModel.changeAddSourceState(AddSourceState.Error(t))
            }
            delay(250)
            uiControlViewModel.changeAddSourceState(AddSourceState.IDLE)
            extensionViewModel.loadAllListExtensionsChannelConfig(true)
        }
    }
}
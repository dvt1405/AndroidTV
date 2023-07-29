package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.asSuccessFlow
import com.kt.apps.media.mobile.viewmodels.features.IFetchIPTVControl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

class IPTVPlaybackInteractor(provider: ViewModelProvider, val coroutineScope: LifecycleCoroutineScope) :
    BasePlaybackInteractor(provider, coroutineScope), IFetchIPTVControl {
    override val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }
    private val _relatedItems = MutableStateFlow<List<ExtensionsChannel>>(emptyList())
    val relatedItems
    get() = _relatedItems
        .map {
            it.map {channel ->
                ChannelElement.ExtensionChannelElement(channel)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    suspend fun loadChannelConfig(configId: String) {
        extensionViewModel.loadChannelForConfig(configId).asSuccessFlow("loadChannelConfig $configId")
            .collectLatest {
                _relatedItems.emit(it)
            }
    }
}
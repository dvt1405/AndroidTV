package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.features.IFetchIPTVControl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

class IPTVPlaybackInteractor(provider: ViewModelProvider, private val coroutineScope: LifecycleCoroutineScope) :
    BasePlaybackInteractor(provider, coroutineScope), IFetchIPTVControl {
    override val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }
    private val _relatedItems = MutableStateFlow<List<ExtensionsChannel>>(emptyList())
    val relatedItems by lazy {
        _relatedItems
            .map {
                it.map {channel ->
                    ChannelElement.ExtensionChannelElement(channel)
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    suspend fun loadChannelConfig(configId: String, group: String) {

        val list = extensionViewModel.loadChannelForConfig(configId).awaitNextValue()
        groupAndSort(list).firstOrNull { pair -> pair.first == group }
            ?.run {
                _relatedItems.emit(second)
            }?: kotlin.run {
            _relatedItems.emit(list)
        }
    }

    fun loadProgramForChanel(channel: ExtensionsChannel): Flow<TVScheduler.Programme> {
        extensionViewModel.loadProgramForChannel(channel, ExtensionsConfig.Type.TV_CHANNEL)
        return extensionViewModel.programmeForChannelLiveData.asUpdateFlow(TAG)
    }
}
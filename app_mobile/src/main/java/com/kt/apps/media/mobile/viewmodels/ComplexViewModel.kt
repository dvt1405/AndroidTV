package com.kt.apps.media.mobile.viewmodels


import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ComplexViewModel(private val provider: ViewModelProvider, scope: CoroutineScope) {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    private val tvChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    private val extensionViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val playbackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus

    val addSourceState
        get() = extensionViewModel.addSourceState
            .onEach { Log.d(TAG, "addSourceState: $it ") }

    val playbackLoadEvent
        get() = playbackViewModel.loadEvents
}
package com.kt.apps.media.mobile.viewmodels


import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.asDataStateFlow
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ComplexInteractors(private val provider: ViewModelProvider, scope: CoroutineScope) {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    private val extensionViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val uiControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    val networkStatus: StateFlow<NetworkState> = networkStateViewModel.networkStatus

    val addSourceState: StateFlow<AddSourceState> by lazy {
        uiControlViewModel.addSourceState
            .stateIn(scope, SharingStarted.WhileSubscribed(), AddSourceState.IDLE)
    }

    val openPlaybackEvent = uiControlViewModel.openPlayback

    val isShowingPlayback by lazy {
        uiControlViewModel.playerState
            .map {
                when(it) {
                    PlaybackState.Fullscreen -> true
                    PlaybackState.Invisible -> false
                    PlaybackState.Minimal -> true
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    val isInPIPMode by lazy {
        uiControlViewModel.isInPIPMode
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    fun onChangePlayerState(state: PlaybackState) {
        uiControlViewModel.changePlayerState(state)
    }

    fun changePiPMode(isEnable: Boolean) {
        uiControlViewModel.changePIPMode(isEnable)
    }
}
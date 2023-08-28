package com.kt.apps.media.mobile.viewmodels


import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.utils.ActivityIndicator
import com.kt.apps.media.mobile.utils.trackActivity
import com.kt.apps.media.mobile.viewmodels.features.IFetchDeepLinkControl
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import com.kt.apps.media.mobile.viewmodels.features.loadByDeepLink
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ComplexInteractors(private val provider: ViewModelProvider, private val scope: CoroutineScope):
    IFetchDeepLinkControl {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    val loadingDeepLink: ActivityIndicator = ActivityIndicator()

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    private val extensionViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    override val uiControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    override val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    override val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
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
                    PlaybackState.Invisible -> false
                    else -> true
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    val isInPIPMode by lazy {
        uiControlViewModel.isInPIPMode
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    val playerState by lazy {
        uiControlViewModel.playerState
            .stateIn(scope, SharingStarted.Eagerly, PlaybackState.Invisible)
    }

    fun onChangePlayerState(state: PlaybackState) {
        uiControlViewModel.changePlayerState(state)
    }

    fun changePiPMode(isEnable: Boolean) {
        uiControlViewModel.changePIPMode(isEnable)
    }

    suspend fun loadChannelDeepLinkJob(deepLink: Uri) {
        scope.launch {
            loadByDeepLink(deepLink)
        }.trackActivity(loadingDeepLink)
    }

    suspend fun openSearch(deepLink: Uri) {
        val filter = deepLink.getQueryParameter("query")
        uiControlViewModel.openSearch(filter ?: "")
    }
}
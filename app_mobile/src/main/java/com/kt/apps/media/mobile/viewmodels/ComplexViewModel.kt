package com.kt.apps.media.mobile.viewmodels


import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.asSuccessFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.sql.DatabaseMetaData
import javax.inject.Inject

sealed class StreamLinkData(
    val title: String,
    val linkStream: List<String>,
    val webDetailPage: String,
    val streamId: String,
    val isHls: Boolean,
    val itemMetaData: Map<String, String>
    ) {
    data class TVStreamLinkData(val data: TVChannelLinkStream): StreamLinkData(
        data.channel.tvChannelName,
        data.linkStream,
        data.channel.tvChannelWebDetailPage,
        data.channel.channelId,
        data.channel.isHls,
        data.channel.getMapData()
    )

    data class ExtensionStreamLinkData(val data: ExtensionsChannel, val streamLink: String): StreamLinkData(
        data.tvChannelName,
        listOf(streamLink),
        data.referer,
        data.channelId,
        data.isHls,
        data.getMapData()
    )
}


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

    val streamData: Flow<StreamLinkData?>
        get() = playbackViewModel.streamLinkData

    val addSourceState
        get() = extensionViewModel.addSourceState
            .onEach { Log.d(TAG, "addSourceState: $it ") }

    init {
        scope.launch {
            tvChannelViewModel.tvWithLinkStreamLiveData.asSuccessFlow("ComplexViewModel")
                .mapLatest { StreamLinkData.TVStreamLinkData(it) }
                .collectLatest { playbackViewModel.startStream(it) }
        }
    }

}
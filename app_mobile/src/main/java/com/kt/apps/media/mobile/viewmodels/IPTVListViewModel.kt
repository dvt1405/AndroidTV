package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.stream.Stream

class IPTVListViewModel(private val provider: ViewModelProvider, private val category: String) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    private val scope = SupervisorJob()
    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async{
            extensionViewModel.loadChannelForConfig(category).asFlow()
                .first()
        }
    }

    suspend  fun loadIPTV(data: ExtensionsChannel) {
        playbackViewModel.changeProcessState(PlaybackViewModel.State.LOADING)
        val linkToPlay = data.tvStreamLink
        if (linkToPlay.contains(".m3u8")) {
            playbackViewModel.startStream(StreamLinkData.ExtensionStreamLinkData(data, linkToPlay))
        } else {
            try {
                val expandUrl = withContext(scope) {
                    linkToPlay.expandUrl()
                }
                playbackViewModel.startStream(StreamLinkData.ExtensionStreamLinkData(data, expandUrl))
            } catch (t: Throwable) {
                playbackViewModel.startStream(StreamLinkData.ExtensionStreamLinkData(data, linkToPlay))
            }
        }
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING)
    }

}
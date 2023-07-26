package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class IPTVListInteractor(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext, private val category: String) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async{
            extensionViewModel.loadChannelForConfig(category).asFlow()
                .first()
        }
    }

    private fun loadIPTV(data: ExtensionsChannel) : StreamLinkData {
        val linkToPlay = data.tvStreamLink
        if (linkToPlay.contains(".m3u8")) {
            return StreamLinkData.ExtensionStreamLinkData(data, linkToPlay)
        } else {
            try {
                val expandUrl = linkToPlay.expandUrl()
                return StreamLinkData.ExtensionStreamLinkData(data, expandUrl)
            } catch (_: Throwable) { }
        }
        return StreamLinkData.ExtensionStreamLinkData(data, linkToPlay)
    }

    suspend fun loadIPTVJob(data: ExtensionsChannel) {
        playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
        playbackViewModel.changeProcessState(PlaybackViewModel.State.LOADING(PrepareStreamLinkData.factory(data)))
        val loadedData = suspendCancellableCoroutine { cont ->
            val result = loadIPTV(data)
            if (cont.isActive) {
                cont.resume(result)
            }
        }
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(loadedData))
    }

}
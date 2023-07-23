package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class IPTVListViewModel(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext, private val category: String) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }
    val processState: Flow<PlaybackViewModel.State>
            get() = playbackViewModel.state

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

//    suspend fun loadIPTVJob(data: ExtensionsChannel): Job {
//        return CoroutineScope(coroutineContext).launch(context = coroutineContext, CoroutineStart.LAZY) {
//            playbackViewModel.stopStream()
//            playbackViewModel.changeProcessState(PlaybackViewModel.State.LOADING)
//            val loadedData = withContext(Dispatchers.Default) {
//                loadIPTV(data)
//            }
//            if (!isActive) {
//                return@launch
//            }
//            playbackViewModel.startStream(loadedData)
//            playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING)
//        }
//    }

    suspend fun loadIPTVJob(data: ExtensionsChannel) {
        Log.d(TAG, "onStartLoading loadIPTVJob: ${data.tvChannelName}")
        playbackViewModel.stopStream()
        playbackViewModel.changeProcessState(PlaybackViewModel.State.LOADING)
        val loadedData = suspendCancellableCoroutine { cont ->
            val result = loadIPTV(data)
            if (cont.isActive) {
                cont.resume(result)
            }
        }
        playbackViewModel.startStream(loadedData)
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING)
    }

}
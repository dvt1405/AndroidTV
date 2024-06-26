package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

interface IFetchIPTVControl: IFetchDataControl {
    val extensionViewModel: ExtensionsViewModel
}

private fun IFetchIPTVControl.parseIPTV(data: ExtensionsChannel): String {
    val linkToPlay = data.tvStreamLink
    if (linkToPlay.contains(".m3u8")) {
        return linkToPlay
    } else {
        try {
            val expandUrl = linkToPlay.expandUrl()
            return expandUrl
        } catch (_: Throwable) { }
    }
    return linkToPlay
}

suspend fun IFetchIPTVControl.loadIPTVJob(data: ExtensionsChannel) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    playbackViewModel.changeProcessState(PlaybackViewModel.State.LOADING(PrepareStreamLinkData.IPTV(data, "")))
    val loadedLink = suspendCancellableCoroutine { cont ->
        val result = CoroutineScope(Dispatchers.Default).async {
            parseIPTV(data)
        }
        if (cont.isActive) {
            cont.resume(result)
        }
    }.await()

    playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(StreamLinkData.ExtensionStreamLinkData(
        data,
        loadedLink
    )))
}

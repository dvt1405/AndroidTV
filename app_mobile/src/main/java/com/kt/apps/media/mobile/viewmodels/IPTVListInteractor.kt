package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.viewmodels.features.IFetchIPTVControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class IPTVListInteractor(private val provider: ViewModelProvider, private val coroutineContext: CoroutineContext, private val category: String)
    : IFetchIPTVControl {
    override val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    override val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async{
            extensionViewModel.loadChannelForConfig(category).asFlow()
                .first()
        }
    }

}
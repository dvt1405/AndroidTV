package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext

class IPTVListInteractor(
    private val provider: ViewModelProvider,
    private val coroutineContext: CoroutineContext,
    private val category: String) : IUIControl
{
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }
    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async{
            extensionViewModel.loadChannelForConfig(category).asFlow()
                .first()
        }
    }



}
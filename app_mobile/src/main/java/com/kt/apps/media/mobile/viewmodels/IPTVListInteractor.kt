package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.CoroutineContext

class IPTVListInteractor(
    private val provider: ViewModelProvider,
    private val coroutineContext: CoroutineContext,
    private val category: String) : IUIControl
{
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }



    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    val isMinimalPlayback by lazy {
        uiControlViewModel.playerState.map { it == PlaybackState.Minimal }
            .stateIn(CoroutineScope(coroutineContext), SharingStarted.WhileSubscribed(), false)
    }
    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async {
            extensionViewModel.loadChannelForConfig(category).await(TAG)
        }
    }



}
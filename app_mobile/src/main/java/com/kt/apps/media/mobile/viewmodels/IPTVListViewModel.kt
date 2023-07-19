package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.utils.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class IPTVListViewModel(private val provider: ViewModelProvider, private val category: String) {
    private val extensionViewModel: ExtensionsViewModel by lazy {
        provider[ExtensionsViewModel::class.java]
    }

    fun loadDataAsync(): Deferred<List<ExtensionsChannel>> {
        return CoroutineScope(Dispatchers.Main).async{
            extensionViewModel.loadChannelForConfig(category).asFlow()
                .first()
        }
    }
}
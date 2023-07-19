package com.kt.apps.media.mobile.viewmodels


import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.asSuccessFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

sealed class StreamLinkData(val title: String) {
    data class TVStreamLinkData(val data: TVChannelLinkStream): StreamLinkData(data.channel.tvChannelName)
}


class ComplexViewModel(private val provider: ViewModelProvider) {
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


    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus

//    private var _streamData: MutableStateFlow<StreamLinkData> = MutableStateFlow()
    val streamData: Flow<StreamLinkData>
        get() = tvChannelViewModel.tvWithLinkStreamLiveData.asSuccessFlow("ComplexViewModel")
            .map { StreamLinkData.TVStreamLinkData(it) }

    val addSourceState
        get() = extensionViewModel.addSourceState
            .onEach { Log.d(TAG, "addSourceState: $it ") }
}
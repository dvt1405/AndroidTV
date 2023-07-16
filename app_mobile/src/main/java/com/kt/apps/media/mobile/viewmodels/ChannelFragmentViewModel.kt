package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import kotlinx.coroutines.flow.StateFlow

class ChannelFragmentViewModel(private val provider: ViewModelProvider) {

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }

    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus
}
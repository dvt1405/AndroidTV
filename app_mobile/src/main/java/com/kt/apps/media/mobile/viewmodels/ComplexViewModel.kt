package com.kt.apps.media.mobile.viewmodels


import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ComplexViewModel(private val provider: ViewModelProvider) {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val networkStateViewModel: NetworkStateViewModel by lazy {
        provider[NetworkStateViewModel::class.java]
    }


    val networkStatus: StateFlow<NetworkState>
        get() = networkStateViewModel.networkStatus
}
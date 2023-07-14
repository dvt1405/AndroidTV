package com.kt.apps.media.mobile.di.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.WrappedTVChannelViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(TVChannelViewModel::class)
    abstract fun bindTVChannelViewModel(tvChannelViewModel: TVChannelViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PlaybackViewModel::class)
    abstract fun bindPlaybackViewModel(playbackViewModel: PlaybackViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExtensionsViewModel::class)
    abstract fun bindExtensionsViewModel(playbackViewModel: ExtensionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkStateViewModel::class)
    abstract fun bindNetworkStateViewModel(networkStateViewModel: NetworkStateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WrappedTVChannelViewModel::class)
    abstract fun bindWrappedTVChannelViewModel(tvChannelViewModel: WrappedTVChannelViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(
        factory: BaseViewModelFactory
    ): ViewModelProvider.Factory
}
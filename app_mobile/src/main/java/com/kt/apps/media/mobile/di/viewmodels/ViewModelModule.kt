package com.kt.apps.media.mobile.di.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.viewmodels.ComplexViewModel
import com.kt.apps.media.mobile.viewmodels.features.FootballViewModel
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
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
    @ViewModelKey(FootballViewModel::class)
    abstract fun bindFootballViewModel(footballViewModel: FootballViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModels::class)
    abstract fun bindSearchViewModel(footballViewModel: SearchViewModels): ViewModel


    @Binds
    abstract fun bindViewModelFactory(
        factory: BaseViewModelFactory
    ): ViewModelProvider.Factory
}
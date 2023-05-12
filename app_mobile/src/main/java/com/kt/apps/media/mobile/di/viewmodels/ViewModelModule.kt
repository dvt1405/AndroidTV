package com.kt.apps.media.mobile.di.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.media.mobile.di.viewmodels.ViewModelKey
import com.kt.apps.media.mobile.ui.fragments.channels.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
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
    abstract fun bindViewModelFactory(
        factory: BaseViewModelFactory
    ): ViewModelProvider.Factory
}
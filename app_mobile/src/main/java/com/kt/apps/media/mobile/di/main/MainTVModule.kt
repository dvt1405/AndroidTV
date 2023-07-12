package com.kt.apps.media.mobile.di.main

import com.kt.apps.media.mobile.di.viewmodels.ViewModelModule
import com.kt.apps.media.mobile.ui.complex.ComplexActivity
import com.kt.apps.media.mobile.ui.fragments.channels.ChannelFragment
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.dashboard.DashboardFragment
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelFragment
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVChannelList
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVDashboard
import com.kt.apps.media.mobile.ui.playback.PlaybackActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainTVModule {

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playback(): PlaybackActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun  complexActivity(): ComplexActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun channelFragment(): ChannelFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackFragment(): PlaybackFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun addExtensionSourceFragment(): AddExtensionFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun lightweightChannelFragment(): LightweightChannelFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVChannelList(): FragmentTVChannelList

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVDashboard(): FragmentTVDashboard

}
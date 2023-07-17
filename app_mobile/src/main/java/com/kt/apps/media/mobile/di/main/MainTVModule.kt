package com.kt.apps.media.mobile.di.main

import com.kt.apps.media.mobile.di.viewmodels.ViewModelModule
import com.kt.apps.media.mobile.ui.complex.ComplexActivity
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.dashboard.DashboardFragment
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.football.dashboard.FootballDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.football.list.FootballListFragment
import com.kt.apps.media.mobile.ui.fragments.iptv.IptvChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.iptv.IptvDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelFragment
import com.kt.apps.media.mobile.ui.fragments.tv.PerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVDashboard
import com.kt.apps.media.mobile.ui.fragments.tv.RadioPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tv.TVPerChannelListFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.RadioChannelsFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment
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
    internal abstract fun channelTVChannelFragment(): TVChannelsFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun channelRadioChannelFragment(): RadioChannelsFragment
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackFragment(): PlaybackFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun addExtensionSourceFragment(): AddExtensionFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun lightweightChannelFragment(): LightweightChannelFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVChannelList(): TVPerChannelListFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentRadioChannelList(): RadioPerChannelListFragment
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVDashboard(): FragmentTVDashboard

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentFootballList(): FootballListFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentFootballDashboard(): FootballDashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentIptvDashboard(): IptvDashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentIptvList(): IptvChannelListFragment
}
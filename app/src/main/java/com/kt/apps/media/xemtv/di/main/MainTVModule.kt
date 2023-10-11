package com.kt.apps.media.xemtv.di.main

import com.kt.apps.autoupdate.ui.AppUpdateActivity
import com.kt.apps.autoupdate.ui.FragmentInfo
import com.kt.apps.autoupdate.ui.FragmentQrCode
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelModule
import com.kt.apps.media.xemtv.ui.DialogActivity
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.details.VideoDetailsFragment
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions
import com.kt.apps.media.xemtv.ui.extensions.FragmentDashboardExtensions
import com.kt.apps.media.xemtv.ui.extensions.FragmentExtensions
import com.kt.apps.media.xemtv.ui.extensions.FragmentExtensionsPlayback
import com.kt.apps.media.xemtv.ui.favorite.FavoriteFragment
import com.kt.apps.media.xemtv.ui.football.FootballFragment
import com.kt.apps.media.xemtv.ui.football.FootballPlaybackFragment
import com.kt.apps.media.xemtv.ui.main.DashboardFragment
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.media.xemtv.ui.playback.FragmentProgramSchedule
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import com.kt.apps.media.xemtv.ui.playback.TVPlaybackVideoFragment
import com.kt.apps.media.xemtv.ui.radio.RadioFragment
import com.kt.apps.media.xemtv.ui.search.TVSearchActivity
import com.kt.apps.media.xemtv.ui.search.TVSearchFragment
import com.kt.apps.media.xemtv.ui.tv.BaseTabLayoutFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboardNew
import com.kt.apps.media.xemtv.ui.tv.FragmentTVGrid
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainTVModule {

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailActivity(): DetailsActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dialogActivity(): DialogActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun tvSearchActivity(): TVSearchActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailFragment(): VideoDetailsFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackActivity(): PlaybackActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackFragment(): TVPlaybackVideoFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun tvDashboardFragment(): FragmentTVDashboard

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun footballListFragment(): FootballFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun footballPlaybackFragment(): FootballPlaybackFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun radioFragment(): RadioFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentExtensions(): FragmentExtensions

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentExtensionsPlayback(): FragmentExtensionsPlayback


    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentAddExtensions(): FragmentAddExtensions

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVDashboardNew(): FragmentTVDashboardNew

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentDashboardExtensions(): FragmentDashboardExtensions

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVGrid(): FragmentTVGrid

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentLoading(): BaseTabLayoutFragment.LoadingFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentTVSearch(): TVSearchFragment


    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun appUpdateActivity(): AppUpdateActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentInfo(): FragmentInfo
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentFavorite(): FavoriteFragment
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentQrCode(): FragmentQrCode
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun fragmentProgramSchedule(): FragmentProgramSchedule

}
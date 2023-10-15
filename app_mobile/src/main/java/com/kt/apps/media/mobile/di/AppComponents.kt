package com.kt.apps.media.mobile.di

import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.media.mobile.di.viewmodels.ViewModelModule
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.di.main.MainTVModule
import com.kt.apps.media.mobile.di.workers.WorkerModule
import com.kt.apps.media.mobile.di.AppScope
import com.kt.apps.media.mobile.logger.PlatformLoggerModule
import com.kt.apps.voiceselector.di.VoiceSelectorComponent
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@Component(
    dependencies = [CoreComponents::class,
        TVComponents::class,
        FootballComponents::class,
        VoiceSelectorComponent::class],
    modules = [
        ViewModelModule::class,
        AndroidSupportInjectionModule::class,
        MainTVModule::class,
        AppModule::class,
        PlatformLoggerModule::class,
        ViewModelModule::class
    ]
)
@AppScope
interface AppComponents : AndroidInjector<App> {

    fun exoPlayerManager(): ExoPlayerManagerMobile

    fun actionLogger(): IActionLogger

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun app(app: App): Builder
        fun coreComponents(coreComponents: CoreComponents): Builder
        fun tvComponents(tvComponents: TVComponents): Builder
        fun footballComponent(footballComponents: FootballComponents): Builder
        fun voiceSelectorComponents(voiceSelectorComponent: VoiceSelectorComponent): Builder
        fun build(): AppComponents
    }

}
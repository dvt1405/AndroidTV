package com.kt.apps.media.xemtv.di

import com.kt.apps.autoupdate.di.AppUpdateComponent
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.CoreLoggerModule
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelModule
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.di.logger.PlatformLoggerModule
import com.kt.apps.media.xemtv.di.main.MainTVModule
import com.kt.apps.media.xemtv.di.workers.WorkerModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@Component(
    dependencies = [CoreComponents::class,
        TVComponents::class,
        FootballComponents::class,
        AppUpdateComponent::class],
    modules = [
        ViewModelModule::class,
        AndroidSupportInjectionModule::class,
        MainTVModule::class,
        WorkerModule::class,
        AppModule::class,
        PlatformLoggerModule::class,
    ]
)
@AppScope
interface AppComponents : AndroidInjector<App> {

    fun exoPlayerManager(): ExoPlayerManager
    fun actionLogger(): IActionLogger

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun app(app: App): Builder
        fun coreComponents(coreComponents: CoreComponents): Builder
        fun tvComponents(tvComponents: TVComponents): Builder
        fun footballComponent(footballComponents: FootballComponents): Builder
        fun appUpdateComponent(appUpdateComponent: AppUpdateComponent): Builder
        fun build(): AppComponents
    }

}
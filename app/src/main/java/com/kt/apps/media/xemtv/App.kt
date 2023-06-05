package com.kt.apps.media.xemtv

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.DaggerCoreComponents
import com.kt.apps.core.tv.di.DaggerTVComponents
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.core.workers.AutoRefreshExtensionsChannelWorker
import com.kt.apps.football.di.DaggerFootballComponents
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.media.xemtv.di.AppComponents
import com.kt.apps.media.xemtv.di.DaggerAppComponents
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import java.time.Duration
import javax.inject.Inject

class App : CoreApp() {

    private val _coreComponents by lazy {
        DaggerCoreComponents.builder()
            .application(this)
            .context(this)
            .build()
    }

    private val _tvComponents by lazy {
        DaggerTVComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    private val _footballComponent by lazy {
        DaggerFootballComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    override val coreComponents: CoreComponents
        get() = _coreComponents

    val tvComponents: TVComponents
        get() = _tvComponents

    val footballComponents: FootballComponents
        get() = _footballComponent

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()
        app = this
        (applicationInjector() as AppComponents).inject(this)
        workManager.enqueueUniquePeriodicWork(
            "Refresh_extension_channel",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoRefreshExtensionsChannelWorker>(
                if (BuildConfig.DEBUG) {
                    Duration.ofMinutes(15L)
                } else {
                    Duration.ofHours(1L)
                }
            )
                .build()
        )
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .tvComponents(_tvComponents)
            .coreComponents(_coreComponents)
            .footballComponent(_footballComponent)
            .app(this)
            .build()
    }

    companion object {
        private lateinit var app: App
        fun get() = app
    }


}
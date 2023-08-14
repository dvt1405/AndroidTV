package com.kt.apps.media.xemtv

import androidx.core.os.bundleOf
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.autoupdate.di.DaggerAppUpdateComponent
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

    private var logLowMemory = false

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

    private val _appUpdateComponent by lazy {
        DaggerAppUpdateComponent.builder()
            .coreComponents(_coreComponents)
            .build()
    }
    var startTimeTracker = System.currentTimeMillis()

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
    }

    override fun onRemoteConfigReady() {
        workManager.enqueueUniquePeriodicWork(
            "Refresh_extension_channel",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoRefreshExtensionsChannelWorker>(
                if (BuildConfig.DEBUG) {
                    Duration.ofMinutes(15L)
                } else {
                    Duration.ofMinutes(15L)
                }
            )
                .setInputData(
                    Data.Builder()
                        .putBoolean(AutoRefreshExtensionsChannelWorker.EXTRA_KEY_VERSION_IS_BETA, BuildConfig.isBeta)
                        .build()
                )
                .build()
        )
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .tvComponents(_tvComponents)
            .coreComponents(_coreComponents)
            .footballComponent(_footballComponent)
            .appUpdateComponent(_appUpdateComponent)
            .app(this)
            .build()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        try {
            if (!logLowMemory) {
                (applicationInjector() as AppComponents).actionLogger()
                    .log(
                        "LowMemory", bundleOf(
                            "LiveTimeBeforeLowMemory" to "${System.currentTimeMillis() - startTimeTracker}"
                        )
                    )
                logLowMemory = true
            }
            clearCacheMemory()
        } catch (_: Exception) {
        }
    }

    private fun clearCacheMemory() {
    }

    companion object {
        private lateinit var app: App
        fun get() = app
    }


}
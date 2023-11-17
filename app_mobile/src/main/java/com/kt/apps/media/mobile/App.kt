package com.kt.apps.media.mobile

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.os.bundleOf
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.DaggerCoreComponents
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.getIsVipDb
import com.kt.apps.core.storage.saveIsVipDb
import com.kt.apps.core.tv.di.DaggerTVComponents
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.core.workers.TVEpgWorkers
import com.kt.apps.football.di.DaggerFootballComponents
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.media.mobile.di.AppComponents
import com.kt.apps.media.mobile.di.DaggerAppComponents
import com.kt.apps.media.mobile.di.MobileTVChannelModule
import com.kt.apps.media.mobile.di.MobileVoiceSelectorModule
import com.kt.apps.media.mobile.di.workers.PreloadDataWorker
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.di.DaggerVoiceSelectorComponent
import com.kt.apps.voiceselector.di.VoiceSelectorComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class App : CoreApp(), Configuration.Provider {
    var logLowMemory = false
    var startTimeTracker = System.currentTimeMillis()

    private val _coreComponents by lazy {
        DaggerCoreComponents.builder()
            .application(this)
            .context(this)
            .build()
    }

    private val _tvComponents by lazy {
        DaggerTVComponents.builder()
            .tVChannelModule(MobileTVChannelModule())
            .coreComponents(_coreComponents)
            .build()
    }

    private val _footballComponent by lazy {
        DaggerFootballComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    val appComponents: AppComponents
        get() = applicationInjector() as AppComponents

    override val coreComponents: CoreComponents
        get() = _coreComponents

    val tvComponents: TVComponents
        get() = _tvComponents

    val footballComponents: FootballComponents
        get() = _footballComponent

    private val _voiceSelector: VoiceSelectorComponent by lazy {
        DaggerVoiceSelectorComponent.builder()
            .coreComponents(_coreComponents)
            .voiceSelectorModule(MobileVoiceSelectorModule())
            .build()
    }

    override fun actionLogger(): IActionLogger {
        return (applicationInjector() as AppComponents).actionLogger()
    }


    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager
    @Inject
    lateinit var keyValueStorage: IKeyValueStorage

    override fun onCreate() {
        super.onCreate()
        app = this
        (applicationInjector() as AppComponents).inject(this)
        voiceSelectorManager.registerLifeCycle()
        if (BuildConfig.isBeta && !keyValueStorage.getIsVipDb()) {
            keyValueStorage.saveIsVipDb(true)
        }
    }

    override fun onRemoteConfigReady() {
        if (BuildConfig.isBeta) enqueuePreloadData()
        workManager.enqueueUniquePeriodicWork(
            "RefreshEpgData",
            ExistingPeriodicWorkPolicy.KEEP,
            if (BuildConfig.DEBUG) {
                PeriodicWorkRequestBuilder<TVEpgWorkers>(15, TimeUnit.MINUTES)
            } else {
                PeriodicWorkRequestBuilder<TVEpgWorkers>(1, TimeUnit.HOURS)
            }.setInputData(
                Data.Builder()
                    .putString(
                        TVEpgWorkers.EXTRA_DEFAULT_URL, Firebase.remoteConfig
                            .getString("epg_url").ifEmpty {
                                "http://lichphatsong.xyz/schedule/vthanhtivi_epg.xml"
                            })
                    .build()
            ).build()
        )
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

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)
//        workManager.enqueue(
//            OneTimeWorkRequestBuilder<TVEpgWorkers>()
//                .setInputData(
//                    Data.Builder()
//                        .putString(
//                            TVEpgWorkers.EXTRA_DEFAULT_URL, Firebase.remoteConfig
//                                .getString("epg_url").ifEmpty {
//                                    "http://lichphatsong.xyz/schedule/vthanhtivi_epg.xml"
//                                })
//                        .putBoolean(TVEpgWorkers.EXTRA_FORCE_UPDATE, true)
//                        .build()
//                )
//                .build()
//        )
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .app(this)
            .coreComponents(_coreComponents)
            .tvComponents(_tvComponents)
            .footballComponent(_footballComponent)
            .voiceSelectorComponents(_voiceSelector)
            .build()
    }

    companion object {
        private lateinit var app: App
        fun get() = app
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setDefaultProcessName("com.kt.apps")
            .build()
    }

    private fun enqueuePreloadData() {
        workManager.enqueue(OneTimeWorkRequestBuilder<PreloadDataWorker>()
            .build())
    }


}

fun App.isNetworkAvailable(): Boolean {
    val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // For 29 api or above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
            else ->     false
        }
    }
    // For below 29 api
    else {
        if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
            return true
        }
    }
    return false
}
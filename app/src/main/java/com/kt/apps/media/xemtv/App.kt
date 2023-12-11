package com.kt.apps.media.xemtv

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.bundleOf
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.autoupdate.di.DaggerAppUpdateComponent
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.DaggerCoreComponents
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.tv.di.DaggerTVComponents
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.workers.AutoRefreshExtensionsChannelWorker
import com.kt.apps.core.workers.TVEpgWorkers
import com.kt.apps.football.di.DaggerFootballComponents
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.media.xemtv.di.AppComponents
import com.kt.apps.media.xemtv.di.DaggerAppComponents
import com.kt.apps.media.xemtv.di.module.TVVoiceSelectorModule
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.di.DaggerVoiceSelectorComponent
import com.kt.apps.voiceselector.di.VoiceSelectorComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
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

    private val _voiceSelector: VoiceSelectorComponent by lazy {
        DaggerVoiceSelectorComponent.builder()
            .coreComponents(_coreComponents)
            .voiceSelectorModule(TVVoiceSelectorModule())
            .build()
    }

    var startTimeTracker = System.currentTimeMillis()

    override val coreComponents: CoreComponents
        get() = _coreComponents

    override fun actionLogger(): IActionLogger {
        return (applicationInjector() as AppComponents).actionLogger()
    }

    val tvComponents: TVComponents
        get() = _tvComponents

    val footballComponents: FootballComponents
        get() = _footballComponent

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager

    private var _currentActivity = WeakReference<Activity>(null)
    val currentActivity: Activity?
        get() = _currentActivity.get()

    override fun onCreate() {
        super.onCreate()
        app = this
        (applicationInjector() as AppComponents).inject(this)
        try {
            addShortcuts()
        } catch (_: Exception) {
        }
        voiceSelectorManager.registerLifeCycle()
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        Log.d(TAG, "onActivityResumed: $activity")
        _currentActivity = WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        super.onActivityStopped(activity)
        Log.d(TAG, "onActivityStopped: $activity")
        val current = _currentActivity.get()?.taskId ?: return
        if (activity.taskId == current) {
            _currentActivity.clear()
        }
    }

    private fun addShortcuts() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setPackage("com.kt.apps.media.xemtv")
        intent.action = Intent.ACTION_VIEW

        val shortcutInfo: ShortcutInfoCompat =
            ShortcutInfoCompat.Builder(this, "com.kt.apps.media.xemtv.2")
                .setShortLabel(getString(R.string.shortcut_short_label1))
                .setLongLabel(getString(R.string.shortcut_long_label1))
                .setAlwaysBadged()
                .addCapabilityBinding("actions.intent.OPEN_APP_FEATURE")
                .setIntent(intent)
                .build()


        ShortcutManagerCompat.pushDynamicShortcut(this, shortcutInfo)
    }

    override fun onRemoteConfigReady() {
        workManager.enqueueUniquePeriodicWork(
            "Refresh_extension_channel",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoRefreshExtensionsChannelWorker>(
                if (BuildConfig.DEBUG) {
                    15
                } else {
                    60
                },
                TimeUnit.MINUTES
            )
                .setInputData(
                    Data.Builder()
                        .putBoolean(AutoRefreshExtensionsChannelWorker.EXTRA_KEY_VERSION_IS_BETA, BuildConfig.isBeta)
                        .build()
                )
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            "RefreshEpgData",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<TVEpgWorkers>(
                if (BuildConfig.DEBUG) {
                    15
                } else {
                    60
                },
                TimeUnit.MINUTES
            ).setInputData(
                Data.Builder()
                    .putString(TVEpgWorkers.EXTRA_DEFAULT_URL, Firebase.remoteConfig
                        .getString("epg_url").ifEmpty {
                            "http://lichphatsong.xyz/schedule/vthanhtivi_epg.xml"
                        })
                    .build()
            ).build()
        )
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .tvComponents(_tvComponents)
            .coreComponents(_coreComponents)
            .footballComponent(_footballComponent)
            .appUpdateComponent(_appUpdateComponent)
            .voiceSelectorComponent(_voiceSelector)
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
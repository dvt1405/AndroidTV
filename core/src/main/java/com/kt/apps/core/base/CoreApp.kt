package com.kt.apps.core.base

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import dagger.android.DaggerApplication

abstract class CoreApp : DaggerApplication(), ActivityLifecycleCallbacks, Configuration.Provider {
    abstract val coreComponents: CoreComponents

    abstract fun actionLogger(): IActionLogger

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    override fun onCreate() {
        super.onCreate()
        app = this
        Firebase.initialize(this)
        Firebase.remoteConfig
            .setDefaultsAsync(mapOf(
                Constants.EXTRA_KEY_USE_ONLINE to true,
                Constants.EXTRA_KEY_VERSION_NEED_REFRESH to 1L
            ))
        fetchAndActivateRemoteConfig(3)
        Firebase.remoteConfig.fetch(20)
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Logger.d(this, tag = "FCM", message = "Fetching FCM registration token failed")
                Logger.e(this, tag = "FCM", exception = task.exception ?: Throwable("Fetching FCM registration token failed"))
                return@OnCompleteListener
            }
            val token = task.result
            Logger.d(this, tag = "FCM", "FCM token: $token")

        })
        registerActivityLifecycleCallbacks(this)
    }

    private fun fetchAndActivateRemoteConfig(maxRetryCount: Int) {
        if (maxRetryCount == 0) return
        Firebase.remoteConfig
            .fetchAndActivate()
            .addOnSuccessListener {
                Logger.d(this, tag = "RemoteConfig", message = "Success")
                onRemoteConfigReady()
            }
            .addOnFailureListener {
                Logger.e(this, tag = "RemoteConfig", exception = it)
                fetchAndActivateRemoteConfig(maxRetryCount - 1)
            }
    }

    abstract fun onRemoteConfigReady()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val componentName = activity.componentName.flattenToString()
        if (!backStack.contains(componentName)) {
            activityCount++
            backStack.addLast(componentName)
        } else {
            synchronized(backStack) {
                backStack.remove(componentName)
                backStack.addLast(componentName)
            }
        }
        Logger.d(this, tag = "Activity", message = "onActivityCreated: $componentName," +
                " activityCount: $activityCount")
    }

    override fun onActivityStopped(activity: Activity) {
        val componentName = activity.componentName.flattenToString()
        if (backStack.lastOrNull() == componentName) {
            if (activityCount > 0) {
                activityCount--
            }
            synchronized(backStack) {
                backStack.remove(componentName)
            }
        }
        Logger.d(
            this@CoreApp, tag = "Activity", message = "onActivityStopped: $componentName," +
                    " activityCount: $activityCount"
        )
    }

    override fun onActivityDestroyed(activity: Activity) {
    }


    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }
    private val backStack by lazy {
        ArrayDeque<String>()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }



    companion object {
        var activityCount = 0
        private lateinit var app: CoreApp
        fun getInstance(): CoreApp {
            return app
        }
    }
}
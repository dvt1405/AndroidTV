package com.kt.apps.voiceselector

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.utils.TAG
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.models.Event
import com.kt.apps.voiceselector.models.State
import com.kt.apps.voiceselector.models.VoicePackage
import com.kt.apps.voiceselector.ui.GGVoiceSelectorFragment
import com.kt.apps.voiceselector.ui.VoicePackageInstallDialogFragment
import com.kt.apps.voiceselector.ui.VoiceSearchActivity
import com.kt.apps.voiceselector.usecase.CheckVoiceInput
import com.kt.apps.voiceselector.usecase.VoiceInputInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.ref.WeakReference
import javax.inject.Inject


data class VoiceSelectorInteractor @Inject constructor(
    val checkVoiceInput: CheckVoiceInput
)

@VoiceSelectorScope
class VoiceSelectorManager @Inject constructor(
    private val interactor: VoiceSelectorInteractor,
    private val voicePackage: VoicePackage,
    private val app: CoreApp,
    private val sharedPreferences: SharedPreferences
) {
    private lateinit var lastActivity: WeakReference<Activity>

    private var _cacheLastLaunchIntent: VoiceInputInfo? = null
    private val _event: PublishSubject<Event> = PublishSubject.create()
    private var _state: State = State.IDLE
    var state: State
        set(value) {
            _state =value
        }
        get() = _state

    fun registerLifeCycle() {
        app.registerActivityLifecycleCallbacks(object: Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                _state = State.IDLE
                if (activity is VoiceSearchActivity) {
                    return
                }
                lastActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    private fun queryAndExecute(): Maybe<VoiceInputInfo?> {
        return interactor.checkVoiceInput()
            .doOnSuccess {
                _cacheLastLaunchIntent = it
                executeFetchedData(it)
            }
    }
    fun openVoiceAssistant(): Maybe<Boolean> {
        Log.d(TAG, "openVoiceAssistant: ")
        val lastInfor = _cacheLastLaunchIntent
        return queryAndExecute()
            .map { it.appInfo != null }
    }

    private fun executeFetchedData(infor: VoiceInputInfo?) {
//        Log.d(TAG, "presentSelectorDialog: $infor")
        val appInfor = infor?.appInfo ?: kotlin.run {
            presentSelector(infor)
            return
        }
        val launchIntent = appInfor.launchIntent ?: kotlin.run {
            presentSelector(infor)
            return
        }
        state = State.LaunchIntent
        
        app.applicationContext.startActivity(launchIntent.apply {
            data = Uri.parse(voicePackage.launchData)
            putExtra(EXTRA_CALLING_PACKAGE, app.packageName)
        })
    }

    private fun tryDeepLink(): Boolean {
        return try {
            app.applicationContext.startActivity(
                Intent(voicePackage.action).apply {
                    data = Uri.parse(voicePackage.launchData)
                    flags = FLAG_ACTIVITY_NEW_TASK
                    putExtra(EXTRA_CALLING_PACKAGE, app.packageName)
                }
            )
            true
        } catch (t: Throwable) {
            Log.d(TAG, "tryDeepLink: $t")
            false
        }
    }

    private fun presentSelector(infor: VoiceInputInfo?) {
        if (tryDeepLink()) {
            return
        }
        val activity = if (this::lastActivity.isInitialized) {
            lastActivity.get() as? FragmentActivity
        } else {
            return
        } ?: return

        if (sharedPreferences.getBoolean(GG_ALWAYS, false)) {
            voiceGGSearch()
        } else if (sharedPreferences.getBoolean(GG_LAST_TIME, false)) {
            val modal = GGVoiceSelectorFragment.newInstance()
            state = State.ShowDialog
            modal.show(activity.supportFragmentManager, GGVoiceSelectorFragment.TAG)
        } else {
            state = State.ShowDialog
            val modal = VoicePackageInstallDialogFragment.newInstance()
            modal.show(activity.supportFragmentManager, VoicePackageInstallDialogFragment.TAG)
        }

    }

    fun launchVoicePackageStore() {
        try {
            app.applicationContext.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${voicePackage.packageName}${voicePackage.extraData}")
                ).apply {
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
            )

        } catch (e: Throwable) {
            Log.d(TAG, "launchVoicePackageStore: $e")
            app.applicationContext.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${voicePackage.packageName}${voicePackage.extraData}")
                ).apply {
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }

    fun voiceGGSearch() {
        state = State.LaunchIntent
        app.applicationContext.startActivity(
            Intent(app.applicationContext, VoiceSearchActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
        )
        sharedPreferences.edit(true) {
            putBoolean(GG_LAST_TIME, true   )
        }
    }

    fun subscribeToVoiceSearch(onNext: (Event) -> Unit): Disposable {
        return _event.subscribe(onNext)
    }

    suspend fun ktSubscribeToVoiceSearch(): Flow<Event> {
        return _event.toCoroutine()
    }

    fun emitEvent(event: Event) {
        _event.onNext(event)
    }

    fun turnOnAlwaysGG() {
        sharedPreferences.edit(true) {
            putBoolean(GG_ALWAYS, true)
        }
    }

    companion object {
        private const val GG_LAST_TIME = "key:GG_LAST_TIME"
        private const val GG_ALWAYS = "key:GG_ALWAYS"
        private const val EXTRA_CALLING_PACKAGE = "calling_package_name"
    }
}

suspend fun <T> PublishSubject<T>.toCoroutine(): Flow<T> = callbackFlow {
    val disposable = CompositeDisposable()
    disposable.add(
        this@toCoroutine.subscribe(
            { value ->
                this.trySend(value)
            }, { error ->
                this.close(error)
            }, {
                this.close(null)
            }
        )
    )
    awaitClose { disposable.dispose() }
}

suspend fun <T> BehaviorSubject<T>.toCoroutine(): Flow<T> = callbackFlow {
    val disposable = CompositeDisposable()
    disposable.add(
        this@toCoroutine.subscribe(
            { value ->
                this.trySend(value)
            }, { error ->
                this.close(error)
            }, {
                this.close(null)
            }
        )
    )
    awaitClose { disposable.dispose() }
}
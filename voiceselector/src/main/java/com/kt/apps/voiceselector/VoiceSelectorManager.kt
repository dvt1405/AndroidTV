package com.kt.apps.voiceselector

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.utils.TAG
import com.kt.apps.voiceselector.di.VoiceSelectorComponent
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.ui.VoiceSelectorDialogFragment
import com.kt.apps.voiceselector.usecase.CheckVoiceInput
import com.kt.apps.voiceselector.usecase.VoiceInputInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton


data class VoiceSelectorInteractor @Inject constructor(
    val checkVoiceInput: CheckVoiceInput
)

@VoiceSelectorScope
class VoiceSelectorManager @Inject constructor(
    private val interactor: VoiceSelectorInteractor,
    private val app: CoreApp
) {
    private lateinit var lastActivity: WeakReference<Activity>
    init {
        app.registerActivityLifecycleCallbacks(object: Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                lastActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    fun openVoiceAssistant(): Maybe<Boolean> {
        Log.d(TAG, "openVoiceAssistant: ")
        return interactor.checkVoiceInput()
            .doOnSuccess { infor -> executeFetchedData(infor) }
            .map { it.appInfo != null }
    }

    private fun executeFetchedData(infor: VoiceInputInfo?) {
        Log.d(TAG, "presentSelectorDialog: $infor")
        val appInfor = infor?.appInfo ?: kotlin.run {
            presentSelector(infor)
            return
        }
        val launchIntent = appInfor.launchIntent ?: kotlin.run {
            presentSelector(infor)
            return
        }

        app.applicationContext.startActivity(launchIntent)
    }

    private fun presentSelector(infor: VoiceInputInfo?) {
        val activity = if (this::lastActivity.isInitialized) {
            lastActivity.get() as? FragmentActivity
        } else {
            return
        } ?: return

        val modal = VoiceSelectorDialogFragment()
        modal.show(activity.supportFragmentManager, VoiceSelectorDialogFragment.TAG)
    }
}
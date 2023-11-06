package com.kt.apps.voiceselector.ui

import android.app.Activity
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.leanback.RowsSupportFragment.TAG
import com.kt.apps.voiceselector.R
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.databinding.ActivityVoiceSearchBinding
import com.kt.apps.voiceselector.models.Event
import javax.inject.Inject

class VoiceSearchActivity : BaseActivity<ActivityVoiceSearchBinding>() {

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager

    override val layoutRes: Int = R.layout.activity_voice_search

    override val shouldShowUpdateNotification: Boolean
        get() = false

    override fun initView(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun initAction(savedInstanceState: Bundle?) {
        launchForResult.launch(voiceAppSearchIntent)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private val launchForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        Log.d(TAG, "result: $result")
//        Toast.makeText(this, "launchForResult ${result.resultCode}", Toast.LENGTH_LONG).show()
        when(result.resultCode) {
            Activity.RESULT_OK -> {
                val resultList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                voiceSelectorManager.emitEvent(Event.VoiceResult(resultList?.firstOrNull() ?: ""))
                Log.d(TAG, "result: $resultList")

                this@VoiceSearchActivity.finish()
                overridePendingTransition(0, 0)
            }

            Activity.RESULT_CANCELED -> {
                voiceSelectorManager.emitEvent(Event.Cancel)

                this@VoiceSearchActivity.finish()
                overridePendingTransition(0, 0)
            }

            else -> {
                voiceSelectorManager.emitEvent(Event.Cancel)

                this@VoiceSearchActivity.finish()
                overridePendingTransition(0, 0)
            }
        }
    }

    private val voiceAppSearchIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val searchManager = (getSystemService(Context.SEARCH_SERVICE)) as SearchManager
            val searchableInfo = searchManager.getSearchableInfo(componentName)
            val queryIntent = Intent(Intent.ACTION_SEARCH)
            queryIntent.component = searchableInfo.searchActivity
            intent.extras?.let { queryIntent.putExtras(it) }
            val pending = PendingIntent.getActivity(
                this@VoiceSearchActivity, 0, queryIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Tìm kiếm trên iMedia")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this@VoiceSearchActivity.componentName.flattenToShortString())
            // Add the values that configure forwarding the results
            putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS)
//            }
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 200)
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 100)
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 250)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1)
        }
    }

    companion object {
        const val EXTRA_CALLING_PACKAGE = "extra:calling_package_name"
        const val EXTRA_CALLING_CLASS_NAME = "extra:calling_class_name"
        fun getLaunchIntent(context: Context, lastActivity: Activity? = null) =
            Intent(context, VoiceSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                lastActivity?.componentName?.let {
                    putExtra(EXTRA_CALLING_PACKAGE, it.packageName)
                    putExtra(EXTRA_CALLING_CLASS_NAME, it.className)
                }
            }
    }

}
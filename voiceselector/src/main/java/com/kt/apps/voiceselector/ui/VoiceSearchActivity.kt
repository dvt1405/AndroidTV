package com.kt.apps.voiceselector.ui

import android.app.Activity
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS)
//            }
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 100)
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 250)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1)
        }
    }

}
package com.kt.apps.core.repository

import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.rxjava3.core.Maybe

interface IVoiceSearchManager {
    fun openVoiceAssistant(extraData: Bundle = bundleOf()): Maybe<Boolean>

    companion object {
        const val EXTRA_RESET_SETTING = "extra:reset_setting"
    }
}
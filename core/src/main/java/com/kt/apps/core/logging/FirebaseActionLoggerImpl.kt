package com.kt.apps.core.logging

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class FirebaseActionLoggerImpl @Inject constructor(
    private val analytics: FirebaseAnalytics
) : IActionLogger {

    override fun log(event: String, extras: Bundle) {
        analytics.logEvent(event, extras)
    }

}
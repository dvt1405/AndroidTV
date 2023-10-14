package com.kt.apps.voiceselector.models

import android.content.Intent
import android.graphics.drawable.Drawable

data class VoicePackage(
    val packageName: String,
    val category: String
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)
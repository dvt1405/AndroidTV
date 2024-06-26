package com.kt.apps.voiceselector.models

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes

data class VoicePackage(
    val packageName: String,
    val action: String,
    val category: String,
    val launchData: String = "",
    val extraData: String = "",
    @DrawableRes val icon:  Int?,
    val title: String,
    val description: String
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)

sealed class Event {
    data class VoiceResult(val string: String): Event()
    object Cancel: Event()
}

sealed class State {
    object IDLE: State()
    object LaunchIntent: State()

    object ShowDialog: State()
}

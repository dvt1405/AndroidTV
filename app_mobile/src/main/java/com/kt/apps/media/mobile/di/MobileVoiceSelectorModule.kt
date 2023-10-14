package com.kt.apps.media.mobile.di

import com.kt.apps.voiceselector.di.VoiceSelectorModule
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.models.VoicePackage
import dagger.Provides

class MobileVoiceSelectorModule: VoiceSelectorModule() {
    override fun providesVoicePackage(): VoicePackage = VoicePackage(
        "ai.zalo.kiki.car",
        "android.intent.category.LAUNCHER"
    )
}
package com.kt.apps.voiceselector.di

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.kt.apps.core.logging.ActionLoggerFactory
import com.kt.apps.core.logging.FirebaseActionLoggerImpl
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.voiceselector.models.VoicePackage
import dagger.Module
import dagger.Provides

@Module
open class VoiceSelectorModule {
    @Provides
    @VoiceSelectorScope
    open fun providesVoicePackage(): VoicePackage = VoicePackage(
        "",
        "",
        "",
        "",
        "",
        null,
        "",
        ""
    )

    @Provides
    @VoiceSelectorScope
    open fun providesAndroidTVLogger(analytics: FirebaseAnalytics): FirebaseActionLoggerImpl {
        return FirebaseActionLoggerImpl(analytics)
    }
}

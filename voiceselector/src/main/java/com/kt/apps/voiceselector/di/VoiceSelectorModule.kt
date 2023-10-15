package com.kt.apps.voiceselector.di

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
        null,
        "",
        ""
    )
}

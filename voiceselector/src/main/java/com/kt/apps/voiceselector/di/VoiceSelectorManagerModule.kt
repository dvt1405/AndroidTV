package com.kt.apps.voiceselector.di

import com.kt.apps.core.repository.IVoiceSearchManager
import com.kt.apps.voiceselector.VoiceSelectorManager
import dagger.Binds
import dagger.Module

@Module
abstract class VoiceSelectorManagerModule {
    @Binds
    @VoiceSelectorScope
    abstract fun voiceSearchManager(voiceSelectorManager: VoiceSelectorManager) : IVoiceSearchManager
}
package com.kt.apps.voiceselector.di

import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.repository.IVoiceSearchManager
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.models.VoicePackage
import com.kt.apps.voiceselector.usecase.AppQuery
import com.kt.apps.voiceselector.usecase.CheckVoiceInput
import dagger.Component

@Component(
    modules = [
        VoiceSelectorModule::class,
        VoiceSelectorManagerModule::class],
    dependencies = [CoreComponents::class]
)
@VoiceSelectorScope
interface VoiceSelectorComponent {
    fun providesVoicePackage(): VoicePackage
    fun providesVoiceSelectorManger(): VoiceSelectorManager
    fun checkVoiceInput(): CheckVoiceInput
    fun appQuery(): AppQuery
    fun iVoiceSelectorManager(): IVoiceSearchManager

}
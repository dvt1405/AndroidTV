package com.kt.apps.voiceselector.di

import android.content.Context
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.logging.FirebaseActionLoggerImpl
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.repository.IVoiceSearchManager
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.models.VoicePackage
import com.kt.apps.voiceselector.usecase.AppQuery
import com.kt.apps.voiceselector.usecase.CheckVoiceInput
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule

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
    fun actionLogger(): FirebaseActionLoggerImpl
    fun checkVoiceInput(): CheckVoiceInput
    fun appQuery(): AppQuery
    fun iVoiceSelectorManager(): IVoiceSearchManager

}
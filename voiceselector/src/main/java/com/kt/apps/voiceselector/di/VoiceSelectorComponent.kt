package com.kt.apps.voiceselector.di

import android.content.Context
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.models.VoicePackage
import com.kt.apps.voiceselector.usecase.AppQuery
import com.kt.apps.voiceselector.usecase.CheckVoiceInput
import dagger.BindsInstance
import dagger.Component

@Component(
    modules = [VoiceSelectorModule::class],
    dependencies = [CoreComponents::class]
)
@VoiceSelectorScope
interface VoiceSelectorComponent {
    fun providesVoicePackage(): VoicePackage
    fun providesVoiceSelectorManger(): VoiceSelectorManager
    fun checkVoiceInput(): CheckVoiceInput
    fun appQuery(): AppQuery

}
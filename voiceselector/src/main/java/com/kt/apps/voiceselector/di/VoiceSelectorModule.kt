package com.kt.apps.voiceselector.di

import com.kt.apps.voiceselector.VoiceSelectorManager
import com.kt.apps.voiceselector.models.VoicePackage
import com.kt.apps.voiceselector.ui.VoicePackageInstallDialogFragment
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

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

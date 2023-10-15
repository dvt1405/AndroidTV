package com.kt.apps.media.mobile.di

import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.R
import com.kt.apps.voiceselector.di.VoiceSelectorModule
import com.kt.apps.voiceselector.di.VoiceSelectorScope
import com.kt.apps.voiceselector.models.VoicePackage
import dagger.Provides

class MobileVoiceSelectorModule: VoiceSelectorModule() {
    override fun providesVoicePackage(): VoicePackage = VoicePackage(
        "ai.zalo.kiki.car",
        "android.intent.category.LAUNCHER",
        R.drawable.kiki_logo,
        "Trợ lý giọng nói Kiki",
        "Điều khiển thiết bị, tìm kiếm nội dung bằng tiếng Việt từ bất kỳ đâu"
    )
}
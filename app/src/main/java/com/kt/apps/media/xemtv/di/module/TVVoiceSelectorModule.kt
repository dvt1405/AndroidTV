package com.kt.apps.media.xemtv.di.module

import com.kt.apps.media.xemtv.R
import com.kt.apps.voiceselector.di.VoiceSelectorModule
import com.kt.apps.voiceselector.models.VoicePackage

//class TVVoiceSelectorModule: VoiceSelectorModule() {
//    override fun providesVoicePackage(): VoicePackage = VoicePackage(
//        packageName = "ai.zalo.kiki.tv",
//        action = "android.intent.action.MAIN",
//        category = "android.intent.category.LEANBACK_LAUNCHER",
//        launchData = "kikiassistant://tv",
//        extraData = "&referrer=utm_source%3Dimedia-app%26utm_medium%3Dinapp%26utm_campaign%3Dimedia-inapp-mic",
//        icon = R.drawable.kiki_logo,
//        "Trợ lý giọng nói Kiki",
//        "Điều khiển thiết bị, tìm kiếm nội dung bằng tiếng Việt từ bất kỳ đâu"
//    )
//}

class TVVoiceSelectorModule: VoiceSelectorModule() {
    override fun providesVoicePackage(): VoicePackage = VoicePackage(
        packageName = "ai.zalo.kiki.tv",
        action = "android.intent.action.VIEW",
        category = "android.intent.category.DEFAULT",
        launchData = "kikiassistant://tv",
        extraData = "&referrer=utm_source%3Dimedia-app%26utm_medium%3Dinapp%26utm_campaign%3Dimedia-inapp-mic",
        icon = R.drawable.kiki_logo,
        "Trợ lý giọng nói Kiki",
        "Điều khiển thiết bị, tìm kiếm nội dung bằng tiếng Việt từ bất kỳ đâu"
    )
}
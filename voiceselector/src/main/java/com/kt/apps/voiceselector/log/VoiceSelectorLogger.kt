package com.kt.apps.voiceselector.log

import android.os.Bundle
import androidx.core.os.bundleOf
import com.kt.apps.core.logging.IActionLogger

object VoiceSelectorLog {
    var cachedExtraData: Bundle = bundleOf()
    open class LogFormat(val event: String, val extras: Bundle = bundleOf())
    object VoiceSearchShowDialog : LogFormat(
        "voice_search_show_dialog",
        cachedExtraData
    )

    object VoiceSearchSelectInstallKiki: LogFormat(
        "voice_search_select_install_kiki",
        cachedExtraData
    )

    object VoiceSearchSelectGGOneTime: LogFormat(
        "voice_search_select_gg_one_time",
        cachedExtraData
    )

    object VoiceSearchSelectGGAlways: LogFormat(
        "voice_search_select_gg_always",
        cachedExtraData
    )

    object VoiceSearchStartKikiAuto: LogFormat(
        "voice_search_start_kiki_auto",
        cachedExtraData
    )

    object VoiceSearchStartGGAuto: LogFormat(
        "voice_search_start_gg_auto",
        cachedExtraData
    )
}

internal fun IActionLogger.logVoiceSelector(obj: VoiceSelectorLog.LogFormat, extras: Bundle = bundleOf()) {
    val bundle = bundleOf()
    bundle.putAll(extras)
    bundle.putAll(obj.extras)

    log(obj.event, bundle)
}
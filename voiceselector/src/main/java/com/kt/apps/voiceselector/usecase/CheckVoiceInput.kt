package com.kt.apps.voiceselector.usecase

import android.os.Bundle
import android.util.Log
import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.utils.TAG
import com.kt.apps.voiceselector.models.AppInfo
import com.kt.apps.voiceselector.models.VoicePackage
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject

data class VoiceInputInfo(val appInfo: AppInfo?)
class CheckVoiceInput @Inject constructor(
    private val voicePackage: VoicePackage,
    private val appQuery: AppQuery
): MaybeUseCase<VoiceInputInfo>() {
    override fun prepareExecute(params: Map<String, Any>): Maybe<VoiceInputInfo> {
        Log.d(TAG, "prepareExecute: $voicePackage")
        return appQuery(voicePackage.action, voicePackage.category, voicePackage.launchData).map {
            val appInfor = it.firstOrNull { appInfo ->
                appInfo.packageName == voicePackage.packageName
            }
            Log.d(TAG, "appInfor: $appInfor")
            return@map VoiceInputInfo(appInfor)
        }
    }

    operator fun invoke() = execute(mapOf())

    operator fun invoke(bundle: Bundle) = execute(
        mapOf(
            "bundle" to bundle
        )
    )
}
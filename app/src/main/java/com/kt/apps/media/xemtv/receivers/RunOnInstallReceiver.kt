package com.kt.apps.media.xemtv.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.tvprovider.media.tv.TvContractCompat
import com.kt.apps.core.logging.Logger

class RunOnInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(this, message = intent.action ?: "")
        intent.extras?.keySet()?.forEach {
            Logger.d(this, message = "$it - ${intent.extras?.get(it)}")
        }
        when (intent.action) {
            TvContractCompat.ACTION_INITIALIZE_PROGRAMS -> {

            }
            TvContractCompat.ACTION_PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT,
            TvContractCompat.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED,
            TvContractCompat.ACTION_PREVIEW_PROGRAM_BROWSABLE_DISABLED-> {
                val programId =
                    intent.extras?.getLong(TvContractCompat.EXTRA_PREVIEW_PROGRAM_ID)
                Logger.d(this, message = "User added program $programId to watch next")

            }

            else -> {

            }
        }
    }

}
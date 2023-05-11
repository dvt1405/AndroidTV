package com.kt.apps.core.extensions

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.PrimaryKey
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class ExtensionsChannel(
    val tvGroup: String,
    val logoChannel: String,
    val tvChannelName: String,
    val tvStreamLink: String,
    val sourceFrom: String,
    @PrimaryKey
    val channelId: String,
    val channelPreviewProviderId: Long = -1,
    val isHls: Boolean,
    val catchupSource: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val props: Map<String, String> = mapOf()
) : Parcelable {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}
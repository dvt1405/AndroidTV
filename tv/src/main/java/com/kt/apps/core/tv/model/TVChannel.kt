package com.kt.apps.core.tv.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class TVChannel(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var tvChannelWebDetailPage: String,
    var sourceFrom: String,
    val channelId: String
) : Parcelable {

    val isRadio: Boolean
    get() = radioGroup.contains(tvGroup)
    override fun toString(): String {
        return "{" +
                "tvGroup: $tvGroup," +
                "logoChannel: $logoChannel," +
                "tvChannelName: $tvChannelName," +
                "tvChannelWebDetailPage: $tvChannelWebDetailPage," +
                "sourceFrom: $sourceFrom," +
                "channelId: $channelId" +
                "}"
    }

    companion object {
        private val radioGroup by lazy {
            listOf(TVChannelGroup.VOV.name, TVChannelGroup.VOH.name)
        }
    }
}
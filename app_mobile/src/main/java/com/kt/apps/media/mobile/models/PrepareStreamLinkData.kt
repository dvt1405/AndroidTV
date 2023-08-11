package com.kt.apps.media.mobile.models

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.football.model.FootballMatch

sealed class PrepareStreamLinkData(
    val title: String,
    val streamId: String
) {
    data class TV(val data: TVChannel): PrepareStreamLinkData(
        data.tvChannelName,
        data.channelId
    )

    data class Radio(val data: TVChannel): PrepareStreamLinkData(
        data.tvChannelName,
        data.channelId
    )

    data class IPTV(val data: ExtensionsChannel, val configId: String): PrepareStreamLinkData(
        data.tvChannelName,
        data.channelId
    )

    data class Football(val data: FootballMatch): PrepareStreamLinkData(
        data.getMatchName(),
        data.matchId
    )

    object Empty: PrepareStreamLinkData("", "")
}
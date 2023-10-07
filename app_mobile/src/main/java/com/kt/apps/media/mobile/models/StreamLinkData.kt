package com.kt.apps.media.mobile.models

import com.airbnb.lottie.L
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink

sealed class StreamLinkData(
    val title: String,
    val linkStream: List<LinkStream>,
    val streamId: String,
    val isHls: Boolean,
    val itemMetaData: Map<String, String>
) {
    data class TVStreamLinkData(val data: TVChannelLinkStream): StreamLinkData(
        data.channel.tvChannelName,
        data.linkStream.map { LinkStream(it, data.channel.tvChannelWebDetailPage, data.channel.tvChannelWebDetailPage) },
        data.channel.channelId,
        data.channel.isHls,
        data.channel.getMapData()
    )

    data class ExtensionStreamLinkData(val data: ExtensionsChannel, val streamLink: String): StreamLinkData(
        data.tvChannelName,
        listOf(LinkStream(streamLink, data.referer, streamLink)),
        data.channelId,
        data.isHls,
        data.getMapData()
    )

    data class FootballStreamLinkData(val match: FootballMatch, val matchWithStreamLink: FootballMatchWithStreamLink): StreamLinkData(
        match.getMatchName(),
        matchWithStreamLink.linkStreams.map {
            LinkStream(it.m3u8Link, it.referer, it.m3u8Link)
        },
        match.matchId,
        false,
        match.getMediaItemData()
    )

    class Custom(
        title: String,
        isHls: Boolean,
        linkStream: List<LinkStream>,
        itemMetaData: Map<String, String>,
        streamId: String,
    ): StreamLinkData(
        title, linkStream, streamId, isHls, itemMetaData
    )
}

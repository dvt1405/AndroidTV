package com.kt.apps.media.mobile.models

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream

sealed class StreamLinkData(
    val title: String,
    val linkStream: List<String>,
    val webDetailPage: String,
    val streamId: String,
    val isHls: Boolean,
    val itemMetaData: Map<String, String>,
    override val type: LinkType
): ILinkData {
    data class TVStreamLinkData(val data: TVChannelLinkStream): StreamLinkData(
        data.channel.tvChannelName,
        data.linkStream,
        data.channel.tvChannelWebDetailPage,
        data.channel.channelId,
        data.channel.isHls,
        data.channel.getMapData(),
        type = LinkType.TV
    )

    data class ExtensionStreamLinkData(val data: ExtensionsChannel, val streamLink: String, val category: String): StreamLinkData(
        data.tvChannelName,
        listOf(streamLink),
        data.referer,
        data.channelId,
        data.isHls,
        data.getMapData(),
        type = LinkType.IPTV
    )
}

package com.kt.apps.media.mobile.models

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannel

sealed class PrepareStreamLinkData(
    val title: String,
    val streamId: String,
    override val type: LinkType
): ILinkData {
    data class TV(val data: TVChannel): PrepareStreamLinkData(
        data.tvChannelName,
        data.channelId,
        LinkType.TV
    )

    data class IPTV(val data: ExtensionsChannel): PrepareStreamLinkData(
        data.tvChannelName,
        data.channelId,
        LinkType.IPTV
    )

    object Empty: PrepareStreamLinkData("", "", LinkType.TV)

    companion object {
        inline  fun <reified T> factory(data: T): PrepareStreamLinkData {
            return when(T::class) {
                TVChannel::class -> TV(data as TVChannel)
                ExtensionsChannel::class -> IPTV(data as ExtensionsChannel)
                else -> Empty
            }
        }
    }
}
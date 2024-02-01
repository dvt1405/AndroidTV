package com.kt.apps.core.storage.local.dto

import androidx.room.Entity
import com.kt.apps.core.extensions.ExtensionsChannel

@Entity(
    primaryKeys = ["id", "url"]
)
class VideoFavoriteDTO(
    val id: String,
    val url: String,
    val title: String,
    val category: String,
    val logoUrl: String,
    val sourceFrom: String,
    val type: Type
) {
    enum class Type {
        TV, RADIO, IPTV
    }

    override fun toString(): String {
        return "VideoFavoriteDTO(id='$id', url='$url', title='$title', category='$category', logoUrl='$logoUrl', sourceFrom='$sourceFrom', type=$type)"
    }

    companion object {
        fun fromIPTVChannel(iptvChannel: ExtensionsChannel) = VideoFavoriteDTO(
            id = iptvChannel.channelId,
            url = iptvChannel.tvStreamLink,
            title = iptvChannel.logoChannel,
            category = iptvChannel.tvGroup,
            logoUrl = iptvChannel.logoChannel,
            sourceFrom = iptvChannel.extensionSourceId,
            type = Type.IPTV
        )
    }
}
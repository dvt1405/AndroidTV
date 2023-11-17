package com.kt.apps.core.tv.model

import android.net.Uri
import android.os.Parcelable
import com.google.gson.Gson
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.tv.datasource.impl.MainTVDataSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class TVChannelLinkStream(
    val channel: TVChannel,
    val linkStream: List<TVChannel.Url>
) : Parcelable {
    val linkReadyToStream: List<TVChannel.Url>
        get() {
            return linkStream.filter {
                it.type == MainTVDataSource.TVChannelUrlType.STREAM.value
            }
        }

    val inputExoPlayerLink: List<LinkStream>
        get() = linkStream
            .filter {
                it.type == MainTVDataSource.TVChannelUrlType.STREAM.value
            }
            .filter {
                Uri.parse(it.url).host != null
            }.map {
                LinkStream(
                    it.url,
                    it.referer ?: channel.referer ?: channel.tvChannelWebDetailPage,
                    streamId = channel.channelId,
                    isHls = it.url.contains("m3u8")
                )
            }

    override fun toString(): String {
        return "{" +
                "channel: $channel," +
                "linkStream: ${Gson().toJson(linkStream)}" +
                "}"
    }

    @Parcelize
    data class StreamResolution(
        val type: String,
        val linkStream: String
    ) : Parcelable
}
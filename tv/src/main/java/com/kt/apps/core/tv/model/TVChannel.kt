package com.kt.apps.core.tv.model

import android.os.Parcelable
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.TVChannelEntity
import com.kt.apps.core.storage.local.dto.TVChannelWithUrls
import com.kt.apps.core.tv.datasource.impl.MainTVDataSource
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class TVChannel(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var tvChannelWebDetailPage: String,
    var sourceFrom: String,
    @PrimaryKey
    val channelId: String,
    val urls: List<Url> = listOf(),
    var isFreeContent: Boolean = true,
    var referer: String = ""
) : Parcelable {

    @Ignore
    @IgnoredOnParcel
    var currentProgramme: TVScheduler.Programme? = null

    @Parcelize
    data class Url(
        val dataSource: String? = null,
        val type: String,
        var url: String
    ) : Parcelable {
        val isHls: Boolean
            get() = url.contains("m3u8")
    }

    val isRadio: Boolean
        get() = radioGroup.contains(tvGroup)

    val tvGroupLocalName: String
        get() = TVChannelGroup.valueOf(tvGroup).value

    val isHls: Boolean
        get() = tvChannelWebDetailPage.contains("m3u8")
                || tvGroup != TVChannelGroup.VOV.name

    fun getMapData() = mapOf(
        AbstractExoPlayerManager.EXTRA_MEDIA_ID to channelId,
        AbstractExoPlayerManager.EXTRA_MEDIA_TITLE to tvChannelName,
        AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION to (currentProgramme?.getProgramDescription() ?: ""),
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_TITLE to tvGroup,
        AbstractExoPlayerManager.EXTRA_MEDIA_THUMB to logoChannel,
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_ARTIST to sourceFrom
    )
    fun toChannelDto() = TVChannelDTO(
        tvGroup = tvGroup,
        logoChannel = logoChannel,
        tvChannelName = tvChannelName,
        sourceFrom = sourceFrom,
        channelId = channelId,
        searchKey = tvChannelName.lowercase()
            .replaceVNCharsToLatinChars()
            .removeAllSpecialChars()
    )

    val channelIdWithoutSpecialChars: String
        get() = channelId.removeAllSpecialChars()
            .removePrefix("viechannel")
            .removeSuffix("hd")

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

    override fun equals(other: Any?): Boolean {
        if (other is TVChannel) {
            return other.channelId.equals(channelId, ignoreCase = true)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = tvGroup.hashCode()
        result = 31 * result + logoChannel.hashCode()
        result = 31 * result + tvChannelName.hashCode()
        result = 31 * result + tvChannelWebDetailPage.hashCode()
        result = 31 * result + sourceFrom.hashCode()
        result = 31 * result + channelId.hashCode()
        return result
    }

    companion object {
        private val radioGroup by lazy {
            listOf(TVChannelGroup.VOV.name, TVChannelGroup.VOH.name)
        }

        fun fromEntity(entity: TVChannelEntity) = TVChannel(
            tvChannelName = entity.tvChannelName,
            tvGroup = entity.tvGroup,
            tvChannelWebDetailPage = entity.tvChannelWebDetailPage,
            sourceFrom = entity.sourceFrom,
            channelId = entity.channelId,
            logoChannel = entity.logoChannel.toString(),
            urls = listOf()
        )

        fun fromTVChannelDTO(value: TVChannelDTO) = TVChannel(
            tvChannelName = value.tvChannelName,
            tvGroup = value.tvGroup,
            tvChannelWebDetailPage = "",
            sourceFrom = value.sourceFrom,
            channelId = value.channelId,
            logoChannel = value.logoChannel.toString(),
            urls = listOf()
        )

        fun fromChannelExtensions(entity: ExtensionsChannel) = TVChannel(
            tvChannelName = entity.tvChannelName,
            tvGroup = entity.tvGroup,
            tvChannelWebDetailPage = entity.tvStreamLink,
            sourceFrom = entity.sourceFrom,
            channelId = entity.channelId,
            logoChannel = entity.logoChannel,
            urls = listOf()

        )

        fun TVChannelWithUrls.mapToTVChannel(): TVChannel {
            return TVChannel(
                tvGroup = this.tvChannel.tvGroup,
                tvChannelName = this.tvChannel.tvChannelName,
                tvChannelWebDetailPage = this.urls.firstOrNull {
                    it.type == MainTVDataSource.TVChannelUrlType.WEB_PAGE.value
                }?.url ?: this.urls[0].url,
                urls = this.urls.map { url ->
                    Url(
                        dataSource = url.src,
                        url = url.url,
                        type = url.type
                    )
                },
                sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                logoChannel = this.tvChannel.logoChannel,
                channelId = this.tvChannel.channelId
            )
        }
    }
}
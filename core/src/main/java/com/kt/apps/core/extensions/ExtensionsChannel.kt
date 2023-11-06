package com.kt.apps.core.extensions

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(
    primaryKeys = [
        "channelId",
        "tvStreamLink"
    ]
)
data class ExtensionsChannel(
    var tvGroup: String,
    val logoChannel: String,
    val tvChannelName: String,
    val tvStreamLink: String,
    val sourceFrom: String,
    val channelId: String,
    val channelPreviewProviderId: Long = -1,
    val isHls: Boolean,
    val catchupSource: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val props: Map<String, String>? = null,
    val extensionSourceId: String
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    var currentProgramme: TVScheduler.Programme? = null

    val isValidChannel: Boolean
        get() {
            return tvGroup.isNotBlank() && Uri.parse(tvStreamLink).host != null
                    && !(tvChannelName.contains("Donate")
                    || tvChannelName.lowercase().startsWith("tham gia group")
                    || tvChannelName.lowercase().startsWith("nhóm zalo")
                    )

        }

    fun getMapData() = mapOf(
        AbstractExoPlayerManager.EXTRA_MEDIA_ID to channelId,
        AbstractExoPlayerManager.EXTRA_MEDIA_TITLE to tvChannelName,
        AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION to (currentProgramme?.description?.takeIf {
            it.isNotBlank()
        } ?: tvGroup),
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_TITLE to tvGroup,
        AbstractExoPlayerManager.EXTRA_MEDIA_THUMB to logoChannel,
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_ARTIST to extensionSourceId,
        AbstractExoPlayerManager.EXTRA_MEDIA_SOURCE_TYPE to HistoryMediaItemDTO.Type.IPTV.name
    )

    override fun toString(): String {
        return "{" +
                "channelId=$channelId,\n" +
                "tvGroup=$tvGroup,\n" +
                "logoChannel=$logoChannel,\n" +
                "tvChannelName=$tvChannelName,\n" +
                "tvStreamLink=$tvStreamLink,\n" +
                "sourceFrom=$sourceFrom,\n" +
                "channelPreviewProviderId=$channelPreviewProviderId,\n" +
                "isHls=$isHls,\n" +
                "catchupSource=$catchupSource,\n" +
                "userAgent=$userAgent,\n" +
                "referer=$referer,\n" +
                "extensionSourceId=$extensionSourceId,\n" +
                "props=$props," +
                "currentProgramme: $currentProgramme" +
                "}"
    }
}

class ExtensionsChannelAndConfig(
    @Embedded
    val channel: ExtensionsChannel,
    @Embedded
    val config: ExtensionsConfig
) {
}
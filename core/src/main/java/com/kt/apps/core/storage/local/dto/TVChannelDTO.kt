package com.kt.apps.core.storage.local.dto

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
class TVChannelDTO(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var sourceFrom: String,
    @PrimaryKey
    val channelId: String,
    val searchKey: String
) {
    @Entity(
        primaryKeys = [
            "tvChannelId",
            "url"
        ]
    )
    class TVChannelUrl(
        val src: String? = null,
        val type: String,
        val url: String,
        val tvChannelId: String
    )
}


data class TVChannelWithUrls(
    @Embedded
    val tvChannel: TVChannelDTO,

    @Relation(
        parentColumn = "channelId",
        entityColumn = "tvChannelId"
    )
    val urls: List<TVChannelDTO.TVChannelUrl>
)

@Fts4(
    contentEntity = TVChannelDTO::class,
    order = FtsOptions.Order.ASC,
    matchInfo = FtsOptions.MatchInfo.FTS4,
    tokenizer = "unicode61",
)
@Entity
class TVChannelFts4(
    @PrimaryKey(
        autoGenerate = true
    )
    @ColumnInfo(name = "rowid")
    val index: Int,
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var sourceFrom: String,
    val channelId: String,
    val searchKey: String
)

data class TVChannelFts4WithUrls(
    @Embedded
    val tvChannel: TVChannelDTO,

    @Relation(
        parentColumn = "channelId",
        entityColumn = "tvChannelId"
    )
    val urls: List<TVChannelDTO.TVChannelUrl>
)

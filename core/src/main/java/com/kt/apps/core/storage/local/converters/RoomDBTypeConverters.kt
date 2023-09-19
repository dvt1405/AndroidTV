package com.kt.apps.core.storage.local.converters

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.storage.local.dto.FootballTeamEntity
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO

class RoomDBTypeConverters {

    @TypeConverter
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    fun stringToUri(str: String): Uri = Uri.parse(str)

    @TypeConverter
    fun footballTeamToString(team: FootballTeamEntity): String = Gson().toJson(team)

    @TypeConverter
    fun stringToFootballTeam(str: String): FootballTeamEntity = Gson().fromJson(
        str,
        FootballTeamEntity::class.java
    )

    @TypeConverter
    fun mapToString(map: Map<String, String>?): String = if (map.isNullOrEmpty()) {
        ""
    } else {
        try {
            Gson().toJson(
                map,
                typeToken
            )
        } catch (e: Exception) {
            ""
        }
    }

    @TypeConverter
    fun stringToMap(map: String): Map<String, String>? = if (map.isEmpty()) {
        null
    } else {
        try {
            Gson().fromJson(
                map,
                typeToken
            )
        } catch (e: Exception) {
            null
        }
    }


    @TypeConverter
    fun stringToExtensionsType(str: String): ExtensionsConfig.Type = try {
        ExtensionsConfig.Type
            .valueOf(str)
    } catch (e: java.lang.Exception) {
        ExtensionsConfig.Type.TV_CHANNEL
    }

    @TypeConverter
    fun extensionsTypeToString(type: ExtensionsConfig.Type): String = try {
        type.name
    } catch (e: java.lang.Exception) {
        ExtensionsConfig.Type.TV_CHANNEL.name
    }

    @TypeConverter
    fun stringToHistoryType(str: String): HistoryMediaItemDTO.Type = try {
        HistoryMediaItemDTO.Type
            .valueOf(str)
    } catch (e: java.lang.Exception) {
        HistoryMediaItemDTO.Type.IPTV
    }

    @TypeConverter
    fun historyTypeToString(type: HistoryMediaItemDTO.Type): String = try {
        type.name
    } catch (e: java.lang.Exception) {
        HistoryMediaItemDTO.Type.IPTV.name
    }


    @TypeConverter
    fun stringToVideoFavoriteDTOType(str: String): VideoFavoriteDTO.Type = try {
        VideoFavoriteDTO.Type
            .valueOf(str)
    } catch (e: java.lang.Exception) {
        VideoFavoriteDTO.Type.IPTV
    }

    @TypeConverter
    fun videoFavoriteDTOTypeToString(type: VideoFavoriteDTO.Type): String = try {
        type.name
    } catch (e: java.lang.Exception) {
        VideoFavoriteDTO.Type.IPTV.name
    }

    companion object {
        private val typeToken = TypeToken.getParameterized(
            Map::class.java,
            String::class.java,
            String::class.java
        ).type
    }

}
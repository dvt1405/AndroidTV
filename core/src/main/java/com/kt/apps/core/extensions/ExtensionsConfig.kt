package com.kt.apps.core.extensions

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class ExtensionsConfig(
    val sourceName: String,
    @PrimaryKey
    val sourceUrl: String,
    val type: Type = Type.TV_CHANNEL
) : Parcelable {

    enum class Type {
        TV_CHANNEL, FOOTBALL
    }

    companion object {
        val test by lazy {
            ExtensionsConfig(
                "Test",
                "https://raw.githubusercontent.com/phuhdtv/vietngatv/master/vietngatv.m3u"
            )
        }
        val test2 by lazy {
            ExtensionsConfig(
                "Test K+",
                "https://s.id/nhamng"
            )
        }
    }
}
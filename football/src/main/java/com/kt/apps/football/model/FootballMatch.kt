package com.kt.apps.football.model

import android.os.Parcelable
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.utils.removeAllSpecialChars
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class FootballMatch(
    val homeTeam: FootballTeam,
    val awayTeam: FootballTeam,
    val kickOffTime: String,
    val kickOffTimeInSecond: Long,
    val statusStream: String,
    val detailPage: String,
    val sourceFrom: FootballDataSourceFrom,
    val league: String = "",
    val matchId: String = detailPage
) : Parcelable {
    fun getMatchName(): String {
        return "${homeTeam.name} - ${awayTeam.name}"
    }

    override fun toString(): String {
        return "FootballMatch(homeTeam=$homeTeam, " +
                "awayTeam=$awayTeam, " +
                "kickOffTime='$kickOffTime', " +
                "kickOffTimeInSecond=$kickOffTimeInSecond, " +
                "statusStream='$statusStream', " +
                "detailPage='$detailPage', " +
                "sourceFrom=$sourceFrom, " +
                "league='$league', " +
                "matchId='$matchId')"
    }

    val isLiveMatch: Boolean
        get() {
            val calendar = Calendar.getInstance(Locale.TAIWAN)
            val currentTime = calendar.timeInMillis / 1000
            return (currentTime - kickOffTimeInSecond) > -20 * 60
                    && (currentTime - kickOffTimeInSecond) < 150 * 60
        }

    fun getMediaItemData() = mapOf(
        AbstractExoPlayerManager.EXTRA_MEDIA_ID to matchId,
        AbstractExoPlayerManager.EXTRA_MEDIA_TITLE to getMatchName(),
        AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION to league,
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_TITLE to league,
        AbstractExoPlayerManager.EXTRA_MEDIA_THUMB to awayTeam.logo,
        AbstractExoPlayerManager.EXTRA_MEDIA_ALBUM_ARTIST to sourceFrom.name
    )

    fun getMatchIdForComment() = "${
        homeTeam.name.trim().removeAllSpecialChars()
    }_${awayTeam.name.trim().removeAllSpecialChars()}"
}
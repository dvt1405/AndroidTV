package com.kt.apps.core.extensions.model

import android.os.Parcelable
import android.text.format.DateUtils
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kt.apps.core.utils.DATE_TIME_FORMAT
import com.kt.apps.core.utils.DATE_TIME_FORMAT_0700
import com.kt.apps.core.utils.toDate
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Locale

@Entity
@Parcelize
class TVScheduler @JvmOverloads constructor(
    var date: String = "",
    var sourceInfoName: String = "",
    var generatorInfoName: String = "",
    var generatorInfoUrl: String = "",
    var extensionsConfigId: String = "",
    @PrimaryKey
    var epgUrl: String = ""
) : Parcelable {

    class Channel @JvmOverloads constructor(
        var id: String = "",
        var displayName: String = "",
        var displayNumber: String = "",
        var icon: String = "",
    ) {
        override fun toString(): String {
            return "{" +
                    "channelId: $id,\n" +
                    "displayNumber: $displayNumber,\n" +
                    "displayName: $displayName,\n" +
                    "icon: $icon,\n" +
                    "}"
        }
    }

    @Entity(primaryKeys = ["channel", "title", "start"])
    @Parcelize
    class Programme @JvmOverloads constructor(
        var channel: String = "",
        var channelNumber: String = "",
        var start: String = "",
        var stop: String = "",
        var title: String = "",
        var description: String = "",
        var extensionsConfigId: String = "",
        var extensionEpgUrl: String = ""
    ): Parcelable {
        fun getProgramDescription(): String {
            return description.let {
                var newDesc = it
                for (i in programmeWhiteList) {
                    newDesc = newDesc.replace(Regex("$i này có thời lượng (là |)\\d+ ((giờ \\d+ phút)|phút|giờ|)(\\.|)"), "")
                }
                newDesc
            }.takeIf {
                it.isNotBlank()
            }?.trim() ?: ""
        }

        fun String.getPattern(): String {
            return if (this.contains("+0700")) {
                DATE_TIME_FORMAT_0700
            } else {
                DATE_TIME_FORMAT
            }
        }

        fun isCurrentProgram(): Boolean {
            val currentTime: Long = Calendar.getInstance(Locale.getDefault())
                .timeInMillis
            val pattern = if (start.contains("+0700")) {
                DATE_TIME_FORMAT_0700
            } else {
                DATE_TIME_FORMAT
            }
            val start: Long = if (start.trim() == "+0700") {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.timeInMillis
            } else {
                start.toDate(
                    pattern,
                    Locale.getDefault(),
                    false
                )?.time ?: return false
            }

            val patternStop = if (stop.contains("+0700")) {
                DATE_TIME_FORMAT_0700
            } else {
                DATE_TIME_FORMAT
            }
            val stop: Long = if (stop.trim() == "+0700") {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, 1)
                calendar.set(Calendar.HOUR, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.timeInMillis
            } else {
                stop.toDate(
                    patternStop,
                    Locale.getDefault(),
                    false
                )?.time ?: return false
            }
            if (!DateUtils.isToday(start) && !DateUtils.isToday(stop)) return false
            return currentTime in start..stop
        }

        fun getTime(): String {
            val start = Calendar.getInstance()
            start.timeInMillis = startTimeMilli()
            val end = Calendar.getInstance()
            end.timeInMillis = endTimeMilli()
            val startTime = String.format("%02d:%02d", start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE))
            val endTime = String.format("%02d:%02d", end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE))
            return "$startTime - $endTime"
        }
        fun getStartTime(): String {
            val start = Calendar.getInstance()
            start.timeInMillis = startTimeMilli()
            return String.format("%02d:%02d", start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE))
        }
        fun startTimeMilli() = if (start.trim() == "+0700") {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.timeInMillis
        } else {
            start.toDate(
                start.getPattern(),
                Locale.getDefault(),
                false
            )?.time ?: System.currentTimeMillis()
        }

        fun endTimeMilli() = if (stop.trim() == "+0700") {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.timeInMillis
        } else {
            stop.toDate(
                stop.getPattern(),
                Locale.getDefault(),
                false
            )?.time ?: System.currentTimeMillis()
        }

        override fun toString(): String {
            return "{" +
                    "channel: $channel,\n" +
                    "channelNumber: $channelNumber,\n" +
                    "start: $start,\n" +
                    "stop: $stop,\n" +
                    "title: $title,\n" +
                    "description: $description,\n" +
                    "}"
        }

        fun isToday(): Boolean {
            return DateUtils.isToday(startTimeMilli())
        }
    }

    override fun toString(): String {
        return "{" +
                "date: $date,\n" +
                "sourceInfoName: $sourceInfoName,\n" +
                "generatorInfoName: $generatorInfoName,\n" +
                "sourceInfoUrl: $generatorInfoUrl,\n" +
                "listTV: $extensionsConfigId,\n" +
                "}"
    }

    companion object {
        val programmeWhiteList by lazy {
            arrayOf("[nN]ội dung", "[cC]hương trình")
        }
    }
}
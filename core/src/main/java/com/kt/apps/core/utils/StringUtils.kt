package com.kt.apps.core.utils

import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

val listShortLink by lazy {
    listOf(
        "gg.gg",
        "urlis.net",
        "bitly.com",
        "ow.ly",
        "tinyurl.com",
        "tiny.cc",
        "bit.do",
        "cocc.me"
    )
}
fun String.formatDateTime(
    pattern: String,
    newPattern: String,
    locale: Locale = Locale.getDefault()
): String? {
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.parse(this)?.let {
        SimpleDateFormat(newPattern, locale).format(it)
    } ?: this
}

fun String.toDate(
    pattern: String,
    locale: Locale = Locale.getDefault(),
    isUtc: Boolean = false
): Date? {
    val formatter = SimpleDateFormat(pattern, locale)
    if (isUtc) formatter.timeZone = TimeZone.getTimeZone("UTC")
    return try {
        formatter.parse(this)
    } catch (e: java.lang.Exception) {
        null
    }
}

fun Date.formatDateTime(pattern: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(pattern, locale)
    formatter.timeZone = Calendar.getInstance(locale).timeZone
    return formatter.format(this)
}

fun String.findFirstNumber(): String? {
    val regex = "(\\d+)"
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(this)
    while (matcher.find()) {
        return matcher.group()
    }
    return null
}

fun String.isShortLink(): Boolean {
    return try {
        val host = this.toHttpUrl()
            .host
        host in listShortLink
    } catch (e: Exception) {
        false
    }
}

fun String.expandUrl(): String {
    var conn: HttpURLConnection? = null
    try {
        val inputURL = URL(this)
        conn = inputURL.openConnection() as HttpURLConnection?
        conn?.instanceFollowRedirects = false
        while (conn!!.responseCode / 100 == 3) {
            val location: String = conn.getHeaderField("location");
            conn = URL(location).openConnection() as HttpURLConnection?
        }
        Logger.d(this, message = "Original URL: " + conn.url)
        return conn.url.toString()
    } catch (e: java.lang.Exception) {
        Logger.e(this, exception = e)
    } finally {
        try {
            conn?.disconnect()
        } catch (e: java.lang.Exception) {
            Logger.e(this, exception = e)
        }
    }
    return this
}

const val DATE_TIME_FORMAT_0700 = "yyyyMMddHHmmss +0700"
const val DATE_TIME_FORMAT = "yyyyMMddHHmmss"
fun String.toDateTime(): Date {
    return SimpleDateFormat(DATE_TIME_FORMAT_0700).parse(this)
}

fun String.toDateTimeFormat(format: String): String {
    val date = this.toDateTime()
    return date.formatDateTime(format)
}

fun String.getKeyForLocalLogo() = this.lowercase()
    .trim()
    .replace("•", "")
    .replace(" ", "")
    .removeSuffix("vietteltv")
    .removeSuffix("fpt")
    .removeSuffix("vieon")
    .removeSuffix("tv360")
    .trim()
    .removeSuffix("4k")
    .removeAllSpecialChars()
    .removeSuffix("hd")
    .replace(".", "")

fun String.replaceVNCharsToLatinChars() = this.replace(Regex(Constants.REGEX_VN_A), "a")
    .replace(Regex(Constants.REGEX_VN_D), "d")
    .replace(Regex(Constants.REGEX_VN_E), "e")
    .replace(Regex(Constants.REGEX_VN_I), "i")
    .replace(Regex(Constants.REGEX_VN_O), "o")
    .replace(Regex(Constants.REGEX_VN_U), "u")

fun String.getLatinWords() = this.lowercase()
    .replaceVNCharsToLatinChars()
    .split(" ")
    .filter {
        it.isNotBlank() && it.isNotEmpty()
    }
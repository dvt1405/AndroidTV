package com.kt.apps.core.utils

import com.kt.apps.core.logging.Logger
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
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
    return formatter.parse(this)
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
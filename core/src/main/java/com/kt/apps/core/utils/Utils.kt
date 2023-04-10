package com.kt.apps.core.utils

import android.content.Context
import android.content.pm.Signature
import android.util.Log
import com.google.firebase.ktx.BuildConfig
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.logging.Logger
import io.reactivex.rxjava3.core.Observable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.*
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


fun getHeaderFromLinkStream(referer: String, host: String): Map<String, String> {

    return mutableMapOf(
        "accept-encoding" to "gzip, deflate, br",
        "accept-language" to "vi-VN,vi;q=0.9,fr-FR;q=0.8,fr;q=0.7,en-US;q=0.6,en;q=0.5,am;q=0.4,en-AU;q=0.3",
        "origin" to if (referer.getBaseUrl().trim()
                .isNotEmpty()
        ) referer.getBaseUrl() else referer.removeSuffix("/"),
        "referer" to referer,
        "sec-fetch-dest" to "empty",
        "sec-fetch-site" to "cross-site",
        "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"
    ).apply {
        if (host.isNotEmpty()) {
            "Host" to host.toHttpUrl().host
        }
        if (BuildConfig.DEBUG) {
            Log.e("TAG", this.toString())
        }
    }
}

fun String.getBaseUrl(): String {
    val isUrl = this.contains("http:")
    val isHttps = this.contains("https:")
    if (!isUrl) return ""
    val baseUrl = replace(Regex("(http(s)?:\\/\\/)|(\\/.*)"), "")
    return if (isHttps) "https://$baseUrl" else "http://$baseUrl"
}

private const val SECOND_MILLIS = 1000
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS


fun getTimeAgo(timeInMilli: Long): String {
    var time = timeInMilli
    if (time < 1000000000000L) {
        // if timestamp given in seconds, convert to millis
        time *= 1000
    }
    val now: Long = Calendar.getInstance(Locale.getDefault()).timeInMillis

    val diff = now - time
    return when {
        diff < MINUTE_MILLIS -> {
            "${diff / 1000}s"
        }
        diff < 2 * MINUTE_MILLIS -> {
            "1m"
        }
        diff < 50 * MINUTE_MILLIS -> {
            (diff / MINUTE_MILLIS).toString() + "m"
        }
        diff < 90 * MINUTE_MILLIS -> {
            "1h"
        }
        diff < 24 * HOUR_MILLIS -> {
            (diff / HOUR_MILLIS).toString() + "hrs"
        }
        diff < 48 * HOUR_MILLIS -> {
            "Yesterday"
        }
        else -> {
            (diff / DAY_MILLIS).toString() + "d"
        }
    }
}

fun String.removeAllSpecialChars(): String {
    return this.replace("[^\\dA-Za-z ]", "")
        .replace("\\s+", "+")
        .replace("-", "")
        .trim()
}

fun <T : Any> Observable<T>.log(
    action: () -> Unit,
    actionLogError: (t: Throwable) -> Unit
): Observable<T> {
    action()
    this.doOnError {
        actionLogError(it)
    }
    return this
}

fun Response.doOnSuccess(success: (body: ResponseBody) -> Unit, error: (t: Throwable) -> Unit) {
    val body = this.body
    if (this.isSuccessful && this.code in 200..299 && body != null) {
        success(body)
    } else {
        error(Throwable("Not success with code: ${this.code}"))
    }
}

fun String.toOrigin(): String {
    return this.toHttpUrl().toOrigin()
}

fun HttpUrl.toOrigin(): String {
    return "${this.scheme}://${this.host}/"
}

fun Map<String, String>.buildCookie(): String {
    val cookieBuilder = StringBuilder()
    for (i in this.entries) {
        cookieBuilder.append(i.key)
            .append("=")
            .append(i.value)
            .append(";")
            .append(" ")
    }
    return cookieBuilder.toString().trim().removeSuffix(";")
}

fun Int.dpToPx(): Int {
    return (CoreApp.getInstance().resources.displayMetrics.scaledDensity * this).toInt()
}

fun getToken() {
    val json = JSONObject()
    val time = Date().time.toString()

    json.put("time", time)
    json.put("vip", false)
    json.put("code", 1)

    val signKey = java.lang.StringBuilder(mo9380r(CoreApp.getInstance()))
        .reverse()
        .toString()

    json.put("key", signKey)

    val token = m705f(json.toString())

    Logger.d(
        Any::class.java, "Token", "{" +
                "signKey: $signKey,\n" +
                "time: $time,\n" +
                "token: $token,\n" +
                "json: $json" +
                "" +
                "}"
    )

}


fun m705f(str: String): String? {
    return try {
        val g: SecretKeySpec = m706g("com.monster.truyenhinh.bongda")
        var bArr = ByteArray(16)
        val bytes = str.toByteArray(charset("UTF-8"))
        val instance: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        instance.init(1, g, IvParameterSpec(bArr))
        android.util.Base64.encodeToString(instance.doFinal(bytes), 2)
    } catch (e: UnsupportedEncodingException) {
        throw GeneralSecurityException(e)
    }
}

fun m706g(str: String): SecretKeySpec {
    val instance: MessageDigest = MessageDigest.getInstance("SHA-256")
    val bytes: ByteArray = str.toByteArray(StandardCharsets.UTF_8)
    instance.update(bytes, 0, bytes.size)
    return SecretKeySpec(instance.digest(), "AES")
}

fun mo9380r(context: Context): String {
    var str = ""
    try {
        val signatureArr: Array<Signature> = context.packageManager.getPackageInfo("com.quang.monstertv", 64).signatures
        var i = 0
        val length = signatureArr.size
        while (i < length) {
            val signature: Signature = signatureArr[i]
            i++
            val instance = MessageDigest.getInstance("SHA-1")
            instance.update(signature.toByteArray())
            val a: String = String(instance.digest())
//            C2299a.m12202l(a, "base16().encode(md.digest())")
            Logger.d(Any::class.java,"signatureArr", str)
            str = a
        }
    } catch (unused: Throwable) {
        Logger.e(Any::class.java, exception = unused)
    }
    return str.uppercase(Locale.ROOT)
}

@Throws(Exception::class)
fun decompress(str: String?): String? {
    if (str == null || str.isEmpty()) {
        return str
    }
    Logger.d(Any::class.java, "Input", str)
    val gis = GZIPInputStream(ByteArrayInputStream(str.toByteArray(charset("UTF-8"))))
    val bf = BufferedReader(InputStreamReader(gis, "UTF-8"))
    var outStr = ""
    var line: String
    while (bf.readLine().also { line = it } != null) {
        outStr += line
    }
    Logger.d(Any::class.java, "Output", outStr)
    return outStr
}

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 23) tag else tag.substring(0, 23)
    }
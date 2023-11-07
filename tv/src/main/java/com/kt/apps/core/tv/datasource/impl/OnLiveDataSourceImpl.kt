package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.core.Observable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

class OnLiveDataSourceImpl @Inject constructor(
    private val client: OkHttpClient,
    private val tvStorage: TVStorage
) : ITVDataSource {
    private val oldCookies: MutableMap<String, String> by lazy {
        tvStorage.cacheCookie(TVDataSourceFrom.ON_LIVE).toMutableMap()
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        throw UnsupportedOperationException("Not yet implemented")
    }

    private fun getCookieString(): String {
        return oldCookies.mapTo(mutableListOf()) { (k, v) ->
            "$k=$v"
        }.joinToString(";")
            .removeSuffix(";")
    }


    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> = Observable.create<TVChannelLinkStream> {
        val livePlayer = getMainPage(tvChannel.channelId)
        this.redirectToLivePlayer(livePlayer)
        this.getLive(livePlayer)
        val aid = getAID(livePlayer)
        val linkStream = getLiveStream(livePlayer, aid)
        it.onNext(
            TVChannelLinkStream(
                channel = tvChannel,
                linkStream = listOf(
                    TVChannel.Url.fromUrl(
                        url = linkStream
                    )
                )
            )
        )
        it.onComplete()
    }.doOnError {
        Logger.e(this@OnLiveDataSourceImpl, exception = it)
    }


    private fun getMainPage(channelId: String = "joyfmmhz"): LivePlayer {
        val detailPageUrl = "$URL$channelId"
        val document = Jsoup.connect(detailPageUrl)
            .cookies(oldCookies)
            .execute()
        oldCookies.putAll(document.cookies())
        val body = document.parse()
        body.select("script").forEach {
            if (it.html().contains("LivePlayer")) {
                val szLang = regexExtract(it.html(), "var szLang\\s*=\\s*'([^']*)'")
                val szLocalCode = regexExtract(it.html(), "var szLocalCode\\s*=\\s*'([^']*)'")
                val szBjId = regexExtract(it.html(), "var szBjId\\s*=\\s*'([^']*)'")
                val szBjNick = regexExtract(it.html(), "var szBjNick\\s*=\\s*'([^']*)'")
                val nBroadNo = regexExtract(it.html(), "var nBroadNo\\s*=\\s*(\\d+);")
                println("szLang=$szLang, szLocalCode=$szLocalCode, szBjId=$szBjId, szBjNick=$szBjNick, nBroadNo=$nBroadNo")
                return LivePlayer(szLang, szLocalCode, szBjId, szBjNick, nBroadNo)
            }
        }
        throw IllegalStateException("No Live Player found")
    }

    private fun redirectToLivePlayer(livePlayer: LivePlayer) {
        val livePlayerUrl = "https://play.onlive.vn/${livePlayer.szBjId}/${livePlayer.nBroadNo}"
        val document = Jsoup.connect(livePlayerUrl)
            .cookies(oldCookies)
            .execute()
        oldCookies.putAll(document.cookies())
    }

    private fun getLive(livePlayer: LivePlayer): String {
        val url = "https://live.onlive.vn/afreeca/player_live_api.php?bjid=${livePlayer.szBjId}"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Origin", "https://play.onlive.vn")
            .header("Referer", "https://play.onlive.vn/${livePlayer.szBjId}/${livePlayer.nBroadNo}")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36"
            )
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Cookie", getCookieString())
            .post(
                FormBody.Builder()
                    .add("bid", livePlayer.szBjId)
                    .add("bno", livePlayer.nBroadNo)
                    .add("type", "live") //live, aid
                    .add("pwd", "")
                    .add("player_type", "html5")
                    .add("stream_type", "common")
                    .add("quality", "HD") //HD, original
                    .add("mode", "landing")
                    .add("from_api", "0")
                    .build()
            )
            .build()
        return client.newCall(request)
            .execute()
            .also { putCookie(it) }
            .body
            .string()

    }

    private fun getAID(livePlayer: LivePlayer): String {
        val url = "https://live.onlive.vn/afreeca/player_live_api.php?bjid=${livePlayer.szBjId}"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Origin", "https://play.onlive.vn")
            .header("Referer", "https://play.onlive.vn/${livePlayer.szBjId}/${livePlayer.nBroadNo}")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36"
            )
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Cookie", getCookieString())
            .post(
                FormBody.Builder()
                    .add("bid", livePlayer.szBjId)
                    .add("bno", livePlayer.nBroadNo)
                    .add("type", "aid") //live, aid
                    .add("pwd", "")
                    .add("player_type", "html5")
                    .add("stream_type", "common")
                    .add("quality", "original") //HD, original
                    .add("mode", "landing")
                    .add("from_api", "0")
                    .build()
            )
            .build()
        val response = client.newCall(request)
            .execute()
            .also { putCookie(it) }
            .body
            .string()
        return JSONObject(response).optJSONObject("CHANNEL")?.optString("AID") ?: ""
    }


    private fun getLiveStream(livePlayer: LivePlayer, aid: String): String {
        val url =
            "https://livestream-manager.onlive.vn/broad_stream_assign.html?return_type=gcp_cdn&use_cors=true&cors_origin_url=play.onlive.vn&broad_key=${livePlayer.nBroadNo}-common-original-hls"
        val res = client.newCall(
            Request.Builder()
                .url(url)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36"
                )
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Cookie", getCookieString())
                .build()
        ).execute()
            .also {
                putCookie(it)
            }

        return JSONObject(res.body.string()).optString("view_url")
            .let {
                "$it?aid=$aid"
            }
    }

    private fun putCookie(it: Response) {
        it.headers["Set-Cookie"]
            ?.split(";")
            ?.forEach {
                it.split("=")
                    .takeIf { it.size == 2 }
                    ?.let { cookie ->
                        oldCookies[cookie[0]] = cookie[1]
                    }
            }
    }


    private fun regexExtract(input: String, regex: String): String {
        val pattern: Pattern = Pattern.compile(regex)
        val matcher: Matcher = pattern.matcher(input)

        if (matcher.find()) {
            return try {
                matcher.group(1) ?: matcher.group(0)
            } catch (e: Exception) {
                ""
            }
        }
        return ""
    }

    data class LivePlayer(
        val szLang: String,
        val szLocalCode: String,
        val szBjId: String,
        val szBjNick: String,
        val nBroadNo: String
    )

    companion object {
        private const val URL = "https://play.onlive.vn/"
    }
}
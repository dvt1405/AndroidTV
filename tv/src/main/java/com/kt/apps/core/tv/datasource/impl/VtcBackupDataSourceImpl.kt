package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.ChannelSourceConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.buildCookie
import com.kt.apps.core.utils.doOnSuccess
import com.kt.apps.core.utils.findFirstNumber
import com.kt.apps.core.utils.getBaseUrl
import io.reactivex.rxjava3.core.Observable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject

class VtcBackupDataSourceImpl @Inject constructor(
    private val client: OkHttpClient,
    private val sharePreference: TVStorage,
    private val localDb: RoomDataBase
) : ITVDataSource {
    private val cookies: MutableMap<String, String>
    private val config: ChannelSourceConfig by lazy {
        ChannelSourceConfig(
            baseUrl = "https://portal.vtc.gov.vn/",
            mainPagePath = "live",
            getLinkStreamPath = "StreamChannelPlayer/GetProtectedStreamUrl",
        )
    }

    init {
        val cacheCookie = sharePreference.cacheCookie(TVDataSourceFrom.VTC_BACKUP)
        cookies = cacheCookie.toMutableMap()
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        return Observable.create { emitter ->
            try {
                val homePage = "${config.baseUrl.removeSuffix("/")}/${config.mainPagePath}"
                val jsoup = Jsoup.connect(homePage)
                    .cookies(cookies)
                    .header("Origin", config.baseUrl.removeSuffix("/"))
                    .header("Referer", "${config.baseUrl}/${config.mainPagePath}")
                    .header("User-Agent", Constants.USER_AGENT)
                    .execute()

                val cookie = jsoup.cookies()
                cookies.putAll(cookie)
                val body = jsoup.parse().body()
                val channel = body.getElementById("barTVChannels")!!

                val list = mutableListOf<TVChannel>()
                channel.getElementsByTag("a").forEach {
                    val img = it.getElementsByTag("img").first()!!
                    val onCLick = img.attr("onclick")
                    val src = img.attr("src")
                    val start = onCLick.indexOf("(")
                    val end = onCLick.indexOf(")")
                    val subStr = onCLick.substring(start + 1, end)

                    val attrs = mutableListOf<String>()
                    subStr.split(",").forEach {
                        attrs.add(it.trim().removeSurrounding("'").lowercase())
                    }
                    list.add(
                        TVChannel(
                            tvGroup = TVChannelGroup.VTC.name,
                            tvChannelName = attrs[1],
                            logoChannel = src,
                            tvChannelWebDetailPage = config.baseUrl,
                            sourceFrom = TVDataSourceFrom.VTC_BACKUP.name,
                            channelId = attrs[0]
                        )
                    )
                }
                sharePreference.saveTVByGroup(TVDataSourceFrom.VTC_BACKUP.name, list)
                emitter.onNext(list)
            } catch (e: Exception) {
                emitter.onError(e)
            }
            emitter.onComplete()
        }
    }

    override fun getTvLinkFromDetail(tvChannel: TVChannel, isBackup: Boolean): Observable<TVChannelLinkStream> {
        val channelName = tvChannel.tvChannelName
            .replace("[^\\dA-Za-z ]", "")
            .replace("\\s+", "+")
            .lowercase()
            .removeSuffix("hd")
            .trim()
        Logger.d(this, message = "getTvLinkFromDetail: {" +
                "tvChannel: $tvChannel" +
                "}")

        val savedListChannel = sharePreference.getTvByGroup(TVDataSourceFrom.VTC_BACKUP.name)

        if (savedListChannel.isEmpty()) {
            return getTvList()
                .flatMap { listChannel ->
                    val mapChannel = listChannel.first {
                        channelName.equals(it.tvChannelName, ignoreCase = true)
                    }
                    getTvLinkFromDetail(mapChannel)
                }
        }

        return Observable.create { emitter ->
            val channelId: String? = try {
                sharePreference.getTvByGroup(TVDataSourceFrom.VTC_BACKUP.name).first { channel ->
                    channelName.equals(channel.tvChannelName, ignoreCase = true)
                }.channelId
            } catch (e: Exception) {
                val mapChanel = localDb.mapChannelDao()
                    .getChannelByName(channelName)
                    .blockingFirst()
                    .first()
                val realId = mapChanel.channelId.findFirstNumber()
                try {
                    if (realId!!.toInt() == 16) {
                        "15"
                    } else {
                        realId
                    }
                } catch (e: Exception) {
                    realId
                }
            }

            Logger.d(this, message = "ChannelId: $channelId")

            this.getPlaylistM3u8ById(channelId, { m3u8Url ->
                this.getRealChunks(m3u8Url, { realChunks ->
                    emitter.onNext(
                        TVChannelLinkStream(
                            tvChannel,
                            listOf(realChunks).map {
                                TVChannel.Url.fromUrl(
                                    url = it,
                                    referer = tvChannel.tvChannelWebDetailPage,
                                    origin = tvChannel.tvChannelWebDetailPage.getBaseUrl()
                                )
                            }
                        )
                    )
                    emitter.onComplete()
                }) { emitter.onError(it) }

            }) { emitter.onError(it) }
        }
    }

    private fun getPlaylistM3u8ById(
        id: String?,
        onResponse: (chunkUrl: String) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        id ?: let {
            onError(Throwable("Channel id is null"))
            return
        }
        val requestBody = FormBody.Builder()
            .add("streamID", id)
            .build()
        val realUrl = "${config.baseUrl.removeSuffix("/")}/${config.getLinkStreamPath!!.removeSuffix("/")}"
        val request = Request.Builder()
            .url(realUrl)
            .addHeader("Origin", config.baseUrl.removeSuffix("/"))
            .addHeader("Referer", config.baseUrl)
            .addHeader("User-Agent", Constants.USER_AGENT)
            .addHeader("Cookie", cookies.buildCookie())
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.doOnSuccess({
                    val m3u8Url = it.string()
                        .replace("\\u0026", "&")
                        .removeSurrounding("\"")
                        .trim()
                    onResponse(m3u8Url)
                }, onError)
            }

        })
    }

    private fun getRealChunks(
        m3u8Url: String,
        onResponse: (chunkUrl: String) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        client.newCall(
            Request.Builder()
                .url(m3u8Url)
                .addHeader("Origin", config.baseUrl.removeSuffix("/"))
                .addHeader("Referer", config.baseUrl)
                .addHeader("User-Agent", Constants.USER_AGENT)
                .addHeader("Host", m3u8Url.toHttpUrl().host)
                .build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.doOnSuccess({
                    val res: String = it.string().let {
                        val realChunks = it.split("\n").first {
                            it.trim().isNotEmpty() && !it.trim().startsWith("#")
                        }
                        val index = m3u8Url.indexOf(".m3u8")
                        val subUrl = m3u8Url.substring(0, index + 5)
                        val lastIndex = subUrl.lastIndexOf("/")
                        val host = subUrl.substring(0, lastIndex)
                        "$host/$realChunks"
                    }
                    onResponse(res)
                }, {
                    onResponse(m3u8Url)
                })
            }

        })
    }
}
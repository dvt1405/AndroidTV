package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.Constants
import com.kt.apps.core.tv.FirebaseLogUtils
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.ChannelSourceConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.buildCookie
import com.kt.apps.core.utils.doOnSuccess
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject

class HTVBackUpDataSourceImpl @Inject constructor(
    private val client: OkHttpClient,
    private val sharePreference: TVStorage
) : ITVDataSource {
    private val cookie by lazy {
        mutableMapOf<String, String>()
    }

    private val config: ChannelSourceConfig by lazy {
        ChannelSourceConfig(
            baseUrl = "https://hplus.com.vn/",
            mainPagePath = "tivi-online/",
            getLinkStreamPath = "content/getlinkvideo/",
        )
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        return Observable.create { emitter ->
            val totalChannel = mutableListOf<TVChannel>()

            val document = Jsoup.connect("${config.baseUrl}/${config.mainPagePath}").execute()
            cookie.putAll(document.cookies())
            val body = document.parse().body()
            body.getElementsByClass("panel-wrapper pw-livetv").forEach {
                it.getElementsByTag("a").first()
                    ?.let { a ->
                        val href = a.attr("href")
                        val logo = a.getElementsByTag("img").first()?.attr("src")
                        val id = href
                            .replace(
                                Regex("(xem-kenh-|truyen-hinh-|-full|-hd|hd|.html|-sd|sd)"),
                                ""
                            )
                            .replace(Regex("(-\\d{2,})"), "")
                            .lowercase()
                        totalChannel.add(
                            TVChannel(
                                tvGroup = TVChannelGroup.HTV.name,
                                logoChannel = logo ?: "",
                                tvChannelName = id,
                                tvChannelWebDetailPage = "${config.baseUrl}${href}",
                                sourceFrom = TVDataSourceFrom.HTV_BACKUP.name,
                                channelId = id
                            )
                        )

                    }
            }
            emitter.onNext(totalChannel.distinctBy {
                it.tvChannelWebDetailPage
            })
            emitter.onComplete()
        }.onErrorReturn {
            FirebaseLogUtils.logGetListChannelError(TVDataSourceFrom.HTV_BACKUP.name, it)
            listOf()
        }.doOnNext {
            FirebaseLogUtils.logGetListChannel(TVDataSourceFrom.HTV_BACKUP.name)
        }
    }

    override fun getTvLinkFromDetail(tvChannel: TVChannel, isBackup: Boolean): Observable<TVChannelLinkStream> {

        if (cookie.isEmpty() || sharePreference.getTvByGroup(TVDataSourceFrom.HTV_BACKUP.name).isEmpty()
            || isBackup
        ) {
            return getTvList()
                .flatMap { listChannel ->
                    listChannel.findLast {
                        mapRealChannelToBackupChannel(tvChannel, it, isBackup)
                    }?.let {
                        getTvDetailFromHtmlPage(it)
                    } ?: Observable.error(Throwable(""))
                }
        }

        return getTvDetailFromHtmlPage(tvChannel)
    }

    private fun mapRealChannelToBackupChannel(
        kenhTvDetail: TVChannel,
        backupChannelDetail: TVChannel,
        isBackup: Boolean
    ) = if (backupChannelDetail.channelId.lowercase() == kenhTvDetail.channelId.lowercase()) {
        true
    } else {
        backupChannelDetail.channelId.removeAllSpecialChars().contains(
            kenhTvDetail.channelId
                .removePrefix("vie-channel-")
                .removeAllSpecialChars()
                .lowercase()
                .removeSuffix("hd")
                .trim()
        )
    }

    private fun getTvDetailFromHtmlPage(kenhTvDetail: TVChannel): Observable<TVChannelLinkStream> {
        return Observable.create { emitter ->
            val document = Jsoup.connect(kenhTvDetail.tvChannelWebDetailPage).execute()
            val body = document.parse().body()
            cookie.putAll(document.cookies())
            var link: String? = null
            body.getElementsByTag("script").forEach {
                val html = it.html()

                if (html.contains("var link_stream = iosUrl")) {
                    val regex = "(?<=var\\slink_stream\\s=\\siosUrl\\s=).*?(\".*?\")"
                    val pattern = Pattern.compile(regex)
                    val matcher = pattern.matcher(html)
                    while (matcher.find()) {
                        link = matcher.group(0)
                            ?.trim()
                            ?.removeSuffix("\"")
                            ?.removePrefix("\"")
                        if (link != null && link!!.toHttpUrlOrNull() != null) break
                    }
                }
            }
            if (link == null) {
                emitter.onError(Throwable("Cannot get stream link"))
            } else {
                getLinkStream(link!!, kenhTvDetail, {
                    emitter.onNext(TVChannelLinkStream(kenhTvDetail, it.map {
                        TVChannel.Url.fromUrl(
                            url = it,
                            referer = kenhTvDetail.tvChannelWebDetailPage,
                            origin = kenhTvDetail.tvChannelWebDetailPage.getBaseUrl()
                        )
                    }))
                    emitter.onComplete()
                }, {
                    emitter.onError(it)
                })
            }
        }
    }

    private fun getLinkStream(
        link: String,
        kenhTvDetail: TVChannel,
        onSuccess: (link: List<String>) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        client.newCall(
            Request.Builder()
                .post(
                    FormBody.Builder()
                        .add("url", link)
                        .add("type", "1")
                        .add("is_mobile", "1")
                        .add("csrf_test_name", "")
                        .build()
                )
                .url("${config.baseUrl}${config.getLinkStreamPath}")
                .header("cookie", cookie.buildCookie())
                .header("Origin", config.baseUrl)
                .header("Referer", kenhTvDetail.tvChannelWebDetailPage)
                .build()

        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.doOnSuccess({ body ->
                    getAllChunks(
                        body.string().trim().replace("https //", "https://"),
                        kenhTvDetail,
                        onSuccess,
                        onError
                    )
                }, onError)
            }

        })
    }

    private fun getAllChunks(
        m3u8Url: String,
        tvDetail: TVChannel,
        onSuccess: (realChunks: List<String>) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        client.newCall(
            Request.Builder()
                .url(m3u8Url)
                .addHeader("Origin", config.baseUrl.removeSuffix("/"))
                .addHeader("Referer", tvDetail.tvChannelWebDetailPage)
                .addHeader("Cookie", cookie.buildCookie())
                .addHeader("User-Agent", Constants.USER_AGENT)
                .addHeader("Host", m3u8Url.toHttpUrl().host)
                .build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.doOnSuccess({
                    val realChunks = it.string().split("\n").filter {
                        it.trim().isNotEmpty() && !it.trim().startsWith("#")
                    }
                    val index = m3u8Url.indexOf(".m3u8")
                    val subUrl = m3u8Url.substring(0, index + 5)
                    val lastIndex = subUrl.lastIndexOf("/")
                    val host = subUrl.substring(0, lastIndex)
                    onSuccess(realChunks.map {
                        "$host/$it"
                    })
                }, {
                    onSuccess(listOf(m3u8Url))
                })
            }

        })
    }
}
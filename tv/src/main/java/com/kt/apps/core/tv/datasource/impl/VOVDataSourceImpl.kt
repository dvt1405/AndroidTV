package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.*
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.buildCookie
import com.kt.apps.core.utils.doOnSuccess
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject

class VOVDataSourceImpl @Inject constructor(
    private val client: OkHttpClient,
    private val sharePreference: TVStorage
) : ITVDataSource {
    private val cookie: MutableMap<String, String>

    private val config: ChannelSourceConfig by lazy {
        ChannelSourceConfig(
            baseUrl = "http://vovmedia.vn/",
            mainPagePath = "",
            getLinkStreamPath = ""
        )
    }

    init {
        val cacheCookie = sharePreference.cacheCookie(TVDataSourceFrom.VOV_BACKUP)
        cookie = cacheCookie.toMutableMap()
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        return Observable.create {
            val listChannel = mutableListOf<TVChannel>()
            val connection = Jsoup.connect(config.baseUrl).execute()
            cookie.putAll(connection.cookies())
            val body = connection.parse().body()
            body.getElementsByClass("col-md-3 col-sm-4 p-2 radiologo").forEach {
                try {
                    val link = it.getElementsByTag("a")[0].attr("href")
                    val name = link.toHttpUrl().pathSegments.last()
                    val logo = it.getElementsByTag("source")[0].attr("srcset")
                    listChannel.add(
                        TVChannel(
                            tvGroup = TVChannelGroup.VOV.name,
                            tvChannelName = name,
                            logoChannel = logo,
                            tvChannelWebDetailPage = link,
                            sourceFrom = TVDataSourceFrom.VOV_BACKUP.name,
                            channelId = name
                        )
                    )
                    sharePreference.saveTVByGroup(TVDataSourceFrom.VOV_BACKUP.name, listChannel)
                } catch (_: Exception) {
                }

            }
            it.onNext(listChannel)
            it.onComplete()
        }
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        if (true) {
            return getTvList().flatMap {
                mapToBackupKenhDetail(it, tvChannel)?.let { it1 ->
                    getTvLinkFromDetail(
                        it1,
                        false
                    )
                }
                    ?: Observable.error(Throwable())
            }
        }

        return Observable.create { emitter ->
            val request = Request.Builder()
                .url(tvChannel.tvChannelWebDetailPage)
                .header("cookie", cookie.buildCookie())
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.doOnSuccess({
                        val resStr = it.string()
                        val regex = "(?<=mp3:\\s\").*?(?=\")"
                        val pattern = Pattern.compile(regex)
                        val matcher = pattern.matcher(resStr)
                        val listUrl = mutableListOf<String>()
                        while (matcher.find()) {
                            matcher.group(0)?.let { it1 -> listUrl.add(it1) }
                        }
                        emitter.onNext(
                            TVChannelLinkStream(tvChannel, listUrl.map {
                                TVChannel.Url.fromUrl(it)
                            })
                        )
                        emitter.onComplete()
                    }, {
                        emitter.onError(it)
                    })
                }

            })
        }
    }

    private fun mapToBackupKenhDetail(
        totalChannel: List<TVChannel>,
        kenhTvDetail: TVChannel
    ): TVChannel? {
        return try {
            totalChannel.last {
                it.channelId.lowercase().removeAllSpecialChars().trim()
                    .contains(kenhTvDetail.channelId.removeAllSpecialChars().lowercase().trim())
            }
        } catch (_: Exception) {
            return null
        }
    }
}
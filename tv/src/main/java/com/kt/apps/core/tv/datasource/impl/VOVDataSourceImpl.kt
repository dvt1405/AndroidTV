package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.ChannelSourceConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
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
            val connection = try {
                Jsoup.connect(config.baseUrl)
                    .cookies(cookie)
                    .execute()
            } catch (e: Exception) {
                it.onError(e)
                return@create
            }
            cookie.putAll(connection.cookies())
            val body = connection.parse().body()
            body.select(".row .col").forEach {
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
        if (cookie.isEmpty() || isBackup) {
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
            try {
                val connection = Jsoup.connect(tvChannel.tvChannelWebDetailPage).execute()
                cookie.putAll(connection.cookies())
                val body = connection.parse().body()
                val listUrl = mutableListOf<String>()
                body.select("script").forEach {
                    val regex = "(?<=url: \").*?(?=\")"
                    val pattern = Pattern.compile(regex)
                    val matcher = pattern.matcher(it.html())
                    while (matcher.find()) {
                        matcher.group(0)?.let { it1 -> listUrl.add(it1) }
                    }
                }
                emitter.onNext(
                    TVChannelLinkStream(tvChannel, listUrl.map {
                        TVChannel.Url.fromUrl(it)
                    })
                )
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
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
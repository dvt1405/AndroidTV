package com.kt.apps.core.tv.datasource.impl

import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import io.reactivex.rxjava3.core.Observable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class HYDataSourceImpl @Inject constructor(
    private val _client: OkHttpClient
) : ITVDataSource {
    override fun getTvList(): Observable<List<TVChannel>> {
        throw IllegalStateException("Not implement in child datasource")
    }

    override fun getTvLinkFromDetail(tvChannel: TVChannel, isBackup: Boolean): Observable<TVChannelLinkStream> {
        return Observable.create<TVChannelLinkStream> {
            if (!it.isDisposed) {
                it.onNext(getJson(tvChannel, "${System.currentTimeMillis() / 1000}"))
                it.onComplete()
            }
        }.retry(3)
    }

    private fun getJson(
        tvChannel: TVChannel,
        timeStamp: String = "${System.currentTimeMillis() / 1000}"
    ): TVChannelLinkStream {
        val request = Request.Builder()
            .url("https://hungyentv.vn/schedule/video/$timeStamp")
            .header("Origin", "https://hungyentv.vn")
            .header("Referer", "https://hungyentv.vn/")
            .post(
                FormBody.Builder()
                    .addEncoded("item_id", "")
                    .build()
            )
            .build()

        val listLinkStream = mutableListOf<String>()

        val response = _client.newBuilder()
            .followRedirects(true)
            .build()
            .newCall(request)
            .execute()

        if (response.isSuccessful) {
            val responseStr = response.body.string()
            val jsonBody = JSONObject(responseStr)
            val items = jsonBody.optJSONArray("items") ?: throw IllegalStateException("Empty items found!")
            for (i in 0 until items.length()) {
                val child = items.optJSONObject(i) ?: continue
                val linkStream = child.optString("ott_url")
                if (!linkStream.isNullOrEmpty() && linkStream.contains("m3u8")) {
                    listLinkStream.add(linkStream)
                }
            }
        }
        if (listLinkStream.isEmpty()) throw IllegalStateException("Empty link stream found!")
        return TVChannelLinkStream(
            tvChannel,
            listLinkStream
        )
    }

    data class ItemClassName(
        val activeClass: String,
        val after: Int,
        val canplay: Boolean,
        val canplayClass: String,
        val cantext: String,
        val current: Int,
        val dateplay: String,
        val description: String,
        val duration: Int,
        val frame: String,
        val item_id: String,
        val live: Boolean,
        val liveClass: String,
        val liveTick: String,
        val ott_url: String,
        val program: String,
        val sync: Int,
        val time_begin: Int,
        val undefined: String
    )


}
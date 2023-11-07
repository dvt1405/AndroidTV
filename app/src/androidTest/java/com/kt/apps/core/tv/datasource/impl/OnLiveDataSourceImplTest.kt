package com.kt.apps.core.tv.datasource.impl

import androidx.test.platform.app.InstrumentationRegistry
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.core.Observable
import org.json.JSONObject
import org.junit.Test

class OnLiveDataSourceImplTest : BaseDataSourceTest() {

    lateinit var onLiveDataSourceImpl: OnLiveDataSourceImpl
    lateinit var dataTest: MutableList<TVChannel>
    override fun prepare() {
        super.prepare()
        onLiveDataSourceImpl = OnLiveDataSourceImpl(
            okHttpClient,
            TVStorage(sharedPreferences),
        )

        dataTest =
            InstrumentationRegistry.getInstrumentation().context.resources.assets.open("new_firebase_database_channel_list_tv.json")
                .bufferedReader().use {
                    it.readText()
                }.let {
                    JSONObject(it)
                }.optJSONArray("AllChannels")!!.let {
                    (0 until it.length()).map { index ->
                        it.optJSONObject(index)
                    }
                }.filter {
                    it.optString("group") == "VTVCabOn"
                }.map {
                    TVChannel(
                        tvGroup = it.optString("group"),
                        logoChannel = it.optString("logo"),
                        tvChannelName = it.optString("name"),
                        tvChannelWebDetailPage = it.optString("web"),
                        sourceFrom = it.optString("sourceFrom"),
                        channelId = it.optString("id"),
                        isFreeContent = it.optBoolean("isFreeContent"),
                        referer = it.optString("referer"),
                        urls = it.optJSONArray("urls")?.let { urls ->
                            (0 until urls.length()).map { index ->
                                urls.optJSONObject(index)
                            }
                        }?.map { url ->
                            TVChannel.Url(
                                type = url.optString("type"),
                                url = url.optString("url"),
                                dataSource = url.optString("src")
                            )
                        } ?: listOf()
                    )
                }.toMutableList()
    }

    @Test
    fun getTvLinkFromDetail() {
        Observable.fromIterable(dataTest)
            .flatMap {
                onLiveDataSourceImpl.getTvLinkFromDetail(it)
            }.reduce { t1, t2 ->
                t1.copy(linkStream = t1.linkStream + t2.linkStream)
            }.test()
            .assertValue {
                println("=====================================")
                it.linkStream.map {
                    it.url
                }.forEach {
                    println(it)
                }
                it.linkStream.size >= dataTest.size
            }
    }

    companion object {
        private val dataTest by lazy {

        }
    }

}
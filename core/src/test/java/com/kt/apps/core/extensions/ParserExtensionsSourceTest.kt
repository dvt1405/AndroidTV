package com.kt.apps.core.extensions

import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageForTesting
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit


class ParserExtensionsSourceTest {
    private val testUrl2 = "http://m3u.at/SamsungTV"
    private val testUrl1 = "https://s.id/nhamng"
    private lateinit var parserExtensionsSource: ParserExtensionsSource
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var storage: IKeyValueStorage
    private lateinit var config: ExtensionsConfig
    private lateinit var disposable: CompositeDisposable

    @Before
    fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        config = ExtensionsConfig(
            "IP TV",
            testUrl1
        )
        storage = KeyValueStorageForTesting()
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
        disposable = CompositeDisposable()

    }

    @Test fun testStream() {
        val start = System.currentTimeMillis()
        println("Start: $start")
        val call = okHttpClient
            .newBuilder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            .newCall(
                Request.Builder()
                    .url("https://hqth.me/phimle")
                    .build()

            ).execute()
        val reader = call.body.byteStream()
            .bufferedReader()

        var line = reader.readLine()
        while (line != null) {
            println(line)
            line = reader.readLine()
        }
        println("Time: ${System.currentTimeMillis() - start}")
    }

    @Test
    fun parseFromRemoteRx() {
        parserExtensionsSource.parseFromRemoteRx(config)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { result ->
                result.forEach {
                    println("")
                    println(it)
                    println("")
                }
                println(result.size)
                result.size == 209
            }
    }

    @Test
    fun parseFromRemoteRxList() {
        val listData = listOf(
            "https://vthanhtivi.pw",
            "https://gg.gg/coocaa",
            "https://gg.gg/bearlivetv",
            "https://gg.gg/PHAPTX5",
            "https://bit.ly/beartvplay",
            "https://cvmtv.site",
            "https://gg.gg/khangg",
            "https://s.id/bearlivetv",
            "https://s.id/nhamng",
            "https://antmedia.anttv.xyz",
            "https://bit.ly/vietteliptv",
            "https://gg.gg/Coban66",
            "https://gg.gg/SN-tv",
            "https://hqth.me/fptphimle",
            "https://hqth.me/tvhaypl",
            "https://hqth.me/tvhaypb",
            "https://s.id/phimiptv",
            "https://s.id/ziptvvn",
            "https://gg.gg/phimiptv",
            "https://hqth.me/tvhayphimle",
            "https://gg.gg/vn360sport",
            "https://gg.gg/90phuttv",
            "https://gg.gg/SN-90phut",
            "https://gg.gg/SN-thethao",
            "https://gg.gg/SN-thapcam"
        )
        listData.map {
            ExtensionsConfig(
                sourceName = it.replace("https://", "").replace("http://", ""),
                sourceUrl = it
            )
        }.forEachIndexed { index, extensionsConfig ->
            parserExtensionsSource.parseFromRemoteRx(extensionsConfig)
                .test()
                .assertNoErrors()
                .assertComplete()
        }
    }

    @Test
    fun parseFromRemote() {
        val result = parserExtensionsSource.parseFromRemoteRx(config)
            .blockingGet()
        assert(result?.map {
            it.tvChannelName
        }?.contains("CNN") ?: false)
    }
}
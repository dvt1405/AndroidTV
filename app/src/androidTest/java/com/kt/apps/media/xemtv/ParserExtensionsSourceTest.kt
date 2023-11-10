package com.kt.apps.media.xemtv

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageImpl
import com.kt.apps.core.storage.local.RoomDataBase
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters


@RunWith(Parameterized::class)
class ParserExtensionsSourceTest(
    val config: ExtensionsConfig
) {
    private lateinit var parserExtensionsSource: ParserExtensionsSource
    private lateinit var parserExtensionsProgramSchedule: ParserExtensionsProgramSchedule
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var storage: IKeyValueStorage
    private lateinit var disposable: CompositeDisposable
    private lateinit var context: Context
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = context.getSharedPreferences("Test", Context.MODE_PRIVATE)
        val testDb = Room.inMemoryDatabaseBuilder(
            context, RoomDataBase::class.java
        ).build()
        storage = KeyValueStorageImpl(sharedPreferences)
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
        disposable = CompositeDisposable()
        firebaseApp = FirebaseApp.initializeApp(context)!!
        parserExtensionsProgramSchedule = ParserExtensionsProgramSchedule(
            okHttpClient,
            storage,
            testDb,
            FirebaseRemoteConfig.getInstance(firebaseApp),
            disposable
        )
        parserExtensionsSource = ParserExtensionsSource(
            okHttpClient,
            storage,
            testDb,
            parserExtensionsProgramSchedule,
            FirebaseRemoteConfig.getInstance(firebaseApp)
        )
        println("===================Start Test===================")
        println("config: $config")
    }

    @Test
    fun parseFromRemoteRxList() {
        parserExtensionsSource.parseFromRemoteRx(config)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                println("{Test: $config, size: ${it.size}}")
                it.isNotEmpty()
            }
    }

    companion object {
        private val listData = listOf(
            "https://vthanhtivi.pw",
            "https://gg.gg/coocaa",
            "https://gg.gg/bearlivetv",
            "https://gg.gg/PHAPTX5",
            "https://bit.ly/beartvplay",
            "https://cvmtv.site",
            "https://khanggtivi.xyz",
            "https://s.id/bearlivetv",
            "https://s.id/nhamng",
//            "https://bit.ly/vietteliptv",
//            "https://gg.gg/Coban66",
            "https://hqth.me/fptphimle",
            "https://hqth.me/tvhaypl",
            "https://hqth.me/tvhaypb",
            "https://s.id/phimiptv",
            "https://s.id/ziptvvn",
            "https://gg.gg/phimiptv",
            "https://gg.gg/vn360sport",
            "https://gg.gg/90phuttv",
            "http://m3u.at/SamsungTV",
            "https://bom.to/beartv",
            "https://hqth.me/phimvip"
        )

        @Parameters
        @JvmStatic
        fun data(): Array<ExtensionsConfig> {
            return listData.map {
                ExtensionsConfig(
                    sourceName = it.replace("https://", "").replace("http://", ""),
                    sourceUrl = it
                )
            }.toTypedArray()
        }
    }
}
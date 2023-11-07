package com.kt.apps.core.tv.datasource.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageImpl
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test

class VOVDataSourceImplTest {

    private lateinit var parserExtensionsSource: ParserExtensionsSource
    private lateinit var parserExtensionsProgramSchedule: ParserExtensionsProgramSchedule
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var storage: IKeyValueStorage
    private lateinit var disposable: CompositeDisposable
    private lateinit var context: Context
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vovDataSourceImpl: VOVDataSourceImpl

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
        vovDataSourceImpl = VOVDataSourceImpl(
            okHttpClient,
            TVStorage(sharedPreferences),
        )
        println("===================Start Test===================")
    }


    @Test
    fun getTvList() {
        vovDataSourceImpl.getTvList()
            .test()
            .assertValue {
                it.forEach {
                    println(it.channelId)
                }
                it.isNotEmpty()
            }
    }

    @Test
    fun getTvLinkFromDetail() {
        vovDataSourceImpl.getTvList()
            .flatMapIterable {
                it
            }
            .flatMap {
                vovDataSourceImpl.getTvLinkFromDetail(it)
                    .doOnNext {
                        println("============")
                        println(it)
                    }
            }
            .reduce { t1, t2 ->
                t1.copy(linkStream = t1.linkStream + t2.linkStream)
            }
            .test()
            .assertValue {
                println("============")
                println(it)
                it.linkStream.isNotEmpty()
            }
    }

    companion object {
        private val lisChannelId by lazy {
            listOf(
                "vov1",
                "vov2",
                "vov3",
                "vov-giao-thong-ha-noi",
                "vov-giao-thong-ho-chi-minh",
                "vov-fm89",
                "vov5",
                "vov5-english-247",
                "vov5-mekong",
                "vov4-mien-trung",
                "vov4-tay-bac",
                "vov4-dong-bac",
                "vov4-tay-nguyen",
                "vov4-dong-bang-song-cuu-long",
                "vov4-ho-chi-minh",
                "fm90hanoi"
            )
        }
    }
}
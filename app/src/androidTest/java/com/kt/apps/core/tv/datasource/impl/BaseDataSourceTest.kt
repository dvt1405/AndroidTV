package com.kt.apps.core.tv.datasource.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
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

open class BaseDataSourceTest {
    lateinit var okHttpClient: OkHttpClient
    lateinit var storage: IKeyValueStorage
    lateinit var disposable: CompositeDisposable
    lateinit var context: Context
    lateinit var firebaseApp: FirebaseApp
    lateinit var sharedPreferences: SharedPreferences
    lateinit var vovDataSourceImpl: VOVDataSourceImpl

    @Before
    open fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        context = ApplicationProvider.getApplicationContext()
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

}
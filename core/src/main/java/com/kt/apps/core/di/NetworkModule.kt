package com.kt.apps.core.di

import android.util.Log
import com.kt.apps.core.BuildConfig
import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.DisposableContainer
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import javax.inject.Named

@Module
class NetworkModule {

    private val unCatchExceptionHandler by lazy {
        Thread.UncaughtExceptionHandler { t, e ->
            Log.e("UnCatchExceptionHandler", "{Thead: ${t.name}}")
            Log.e("UnCatchExceptionHandler", e.message, e)
        }
    }

    @Provides
    @CoreScope
    fun providesOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    this.addInterceptor(HttpLoggingInterceptor().apply {
                        this.level = HttpLoggingInterceptor.Level.HEADERS
                    })
                }
            }
            .dispatcher(
                Dispatcher(
                    ForkJoinPool(
                        // max #workers - 1
                        0x7fff.coerceAtMost(Runtime.getRuntime().availableProcessors() * 10),
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                        unCatchExceptionHandler,
                        false
                    )
                )
            )
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @CoreScope
    @Named(EXTRA_NETWORK_DISPOSABLE)
    fun provideNetworkDisposable(): DisposableContainer = CompositeDisposable()

    companion object {
        const val EXTRA_NETWORK_DISPOSABLE = "NetworkDisposable"
    }
}
package com.kt.apps.media.mobile.di

import android.content.Context
import androidx.work.WorkManager
import com.kt.apps.core.logging.ActionLoggerFactory
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.media.mobile.logger.MobileActionLoggerImpl
import com.kt.apps.media.mobile.utils.GlobalExceptionHandler
import dagger.Module
import dagger.Provides

@Module
class AppModule {

    @Provides
    @AppScope
    fun provideWorkerManager(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @AppScope
    fun providesAndroidTVLogger(factory: ActionLoggerFactory): IActionLogger {
        return factory.createLogger(MobileActionLoggerImpl::class.java)
    }

    @Provides
    @AppScope
    fun providesGlobalExceptionHandler(context: Context): Thread.UncaughtExceptionHandler {
        return GlobalExceptionHandler(context)
    }
    @Provides
    @TVScope
    fun providesTimeout(): Long? = 20
}
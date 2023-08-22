package com.kt.apps.media.mobile.di.workers

import androidx.work.ListenableWorker
import dagger.MapKey
import dagger.Module
import kotlin.reflect.KClass

@MapKey
@MustBeDocumented
@Retention()
annotation class WorkerKey(
    val value: KClass<out ListenableWorker>
)

@Module
abstract class WorkerModule {
}

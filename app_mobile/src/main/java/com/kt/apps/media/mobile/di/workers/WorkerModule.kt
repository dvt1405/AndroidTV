package com.kt.apps.media.mobile.di.workers

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import com.kt.apps.core.workers.PreloadDataWorker
import com.kt.apps.core.workers.factory.ChildWorkerFactory
import com.kt.apps.core.workers.factory.MyWorkerFactory
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MapKey
@MustBeDocumented
@Retention()
annotation class WorkerKey(
    val value: KClass<out ListenableWorker>
)

@Module
abstract class WorkerModule {
    @WorkerKey(PreloadDataWorker::class)
    @Binds
    @IntoMap
    abstract fun provideWorkerFactory(factory: PreloadDataWorker.Factory) : ChildWorkerFactory

    @Binds
    abstract fun mainWorkerFactory(
        factory: MyWorkerFactory
    ): WorkerFactory
}

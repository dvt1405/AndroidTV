package com.kt.apps.media.xemtv.di.workers

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers
import com.kt.apps.media.xemtv.workers.factory.ChildWorkerFactory
import com.kt.apps.media.xemtv.workers.factory.MyWorkerFactory
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
    @WorkerKey(TVRecommendationWorkers::class)
    @Binds
    @IntoMap
    abstract fun provideWorkerFactory(factory: TVRecommendationWorkers.Factory): ChildWorkerFactory

    @Binds
    abstract fun mainWorkerFactory(
        factory: MyWorkerFactory
    ): WorkerFactory


}

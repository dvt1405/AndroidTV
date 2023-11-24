package com.kt.apps.core.workers.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

class MyWorkerFactory @Inject constructor(
private val workerFactories: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<ChildWorkerFactory>>
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val foundEntries = workerFactories.entries.find {
            Class.forName(workerClassName).isAssignableFrom(it.key)
        }
        return foundEntries?.value?.get()?.create(appContext, workerParameters)
    }
}
package com.kt.apps.media.mobile.utils

import com.google.android.material.circularreveal.CircularRevealHelper.Strategy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ActivityIndicator {
    private var _replay: MutableStateFlow<Int> = MutableStateFlow(0)
    private val lock = Mutex()

    val isLoading: StateFlow<Boolean>
        get() = _replay.map { it > 0 }
            .distinctUntilChanged()
            .stateIn(
                CoroutineScope(Dispatchers.Main),
                SharingStarted.WhileSubscribed(),
                false
            )

    private suspend fun increment() {
        lock.withLock {
            _replay.emit(_replay.value + 1)
        }
    }

    private suspend fun decrement() {
        lock.withLock {
            _replay.emit(_replay.value - 1)
        }
    }

    suspend fun trackBlock(block: suspend () -> Unit) {
        increment()
        try {
            block()
        } finally {
            decrement()
        }
    }

    fun trackJob(job: Job): Job {
        return CoroutineScope(Dispatchers.Main.immediate).launch {
            increment()
            job.join()
            decrement()
        }
    }

}

fun Job.trackJob(indicator: ActivityIndicator): Job {
    return indicator.trackJob(this)
}

fun CoroutineScope.launchTrack(
    indicator: ActivityIndicator,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> Unit): Job {
    return launch(context, start) {
        indicator.trackBlock(block)
    }

}
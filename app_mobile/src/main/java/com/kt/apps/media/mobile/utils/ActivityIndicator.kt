package com.kt.apps.media.mobile.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    fun <T> trackActivityAsync(job: Deferred<T>): Deferred<T> {
        return CoroutineScope(Dispatchers.Main).async {
            increment()
            job.invokeOnCompletion {
                launch {
                    decrement()
                }
            }
            job.await()
        }
    }

    fun trackActivityAsync(job: Job): Job {
        return MainScope().async {
            increment()
            job.invokeOnCompletion {
                this.launch {
                    decrement()
                }
            }
            job.join()
        }
    }
}

fun <T>Deferred<T>.trackActivity(indicator: ActivityIndicator) : Deferred<T> {
    return indicator.trackActivityAsync(this)
}

fun Job.trackActivity(indicator: ActivityIndicator): Job {
    return indicator.trackActivityAsync(this)
}


package com.kt.apps.core.tv.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVDataSourceFrom
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@TVScope
class GetListTVChannel @Inject constructor(
    private val tvDataSources: Map<TVDataSourceFrom, @JvmSuppressWildcards ITVDataSource>,
    private val timeout: Long?
) : BaseUseCase<List<TVChannel>>() {
    private val isLoadingData by lazy {
        AtomicBoolean()
    }
    private var _pendingSource: Observable<List<TVChannel>>? = null

    override fun prepareExecute(params: Map<String, Any>): Observable<List<TVChannel>> {
        if (cacheData != null && !(params[EXTRA_REFRESH_DATA] as Boolean)) {
            return Observable.just(cacheData!!)
        }

        return (params[EXTRA_TV_SOURCE_FROM]?.let { source ->
            when (source as TVDataSourceFrom) {
                TVDataSourceFrom.GG -> return (tvDataSources[source]!!.getTvList())
                    .onErrorResumeNext {
                        invoke(
                            params[EXTRA_REFRESH_DATA] as Boolean,
                            TVDataSourceFrom.V
                        )
                    }

                TVDataSourceFrom.V -> return (tvDataSources[source]!!.getTvList())
                    .onErrorResumeNext {
                        Observable.concat(
                            tvDataSources[TVDataSourceFrom.VTV_BACKUP]?.getTvList()
                                ?: error("Null data sources ${source.name} provider"),
                            tvDataSources[TVDataSourceFrom.VTC_BACKUP]?.getTvList()
                                ?: error("Null data sources ${source.name} provider"),
                            tvDataSources[TVDataSourceFrom.HTV_BACKUP]?.getTvList()
                                ?: error("Null data sources ${source.name} provider"),
                        )
                    }
                else -> {
                    tvDataSources[source]?.getTvList()
                        ?.onErrorResumeNext {
                            invoke(
                                params[EXTRA_REFRESH_DATA] as Boolean,
                                TVDataSourceFrom.V
                            )
                        }
                        ?: Observable.error(Throwable(""))
                }
            }
        } ?: Observable.concat(
            tvDataSources[TVDataSourceFrom.VTV_BACKUP]?.getTvList()
                ?: error("Null data sources TVDataSourceFrom.VTV_BACKUP provider"),
            tvDataSources[TVDataSourceFrom.VTC_BACKUP]?.getTvList()
                ?: error("Null data sources TVDataSourceFrom.VTC_BACKUP provider"),
            tvDataSources[TVDataSourceFrom.HTV_BACKUP]?.getTvList()
                ?: error("Null data sources TVDataSourceFrom.HTV_BACKUP provider"),
        ))
    }

    operator fun invoke(
        forceRefreshData: Boolean = false,
        sourceFrom: TVDataSourceFrom = TVDataSourceFrom.MAIN_SOURCE,
        timeout: Long? = this.timeout,
    ): Observable<List<TVChannel>> {
        while (isLoadingData.get()) {
            return if (cacheData != null) {
                Observable.just(cacheData!!)
            } else if (_pendingSource != null) {
                _pendingSource!!
            } else {
                Observable.empty()
            }
        }
        isLoadingData.compareAndSet(false, true)
        val listCacheData: MutableList<TVChannel> = mutableListOf()
        val params = mapOf(
            EXTRA_TV_SOURCE_FROM to sourceFrom,
            EXTRA_REFRESH_DATA to forceRefreshData
        )
        return if (timeout == null) {
            execute(params)
        } else {
            execute(params).timeout(timeout, TimeUnit.SECONDS)
        }
            .doOnNext {
                synchronized(this) {
                    listCacheData.addAll(it)
                }
            }
            .also {
                _pendingSource = it
            }
            .doOnComplete {
                _pendingSource = null
                cacheData = listCacheData
                isLoadingData.compareAndSet(true, false)
            }.doOnError {
                _pendingSource = null
                isLoadingData.compareAndSet(true, false)
                cacheData = null
            }
            .onErrorResumeNext { Observable.just(emptyList()) }
    }

    companion object {
        private const val EXTRA_TV_SOURCE_FROM = "extra:tv_source_from"
        private const val EXTRA_REFRESH_DATA = "extra:refresh_data"
    }
}
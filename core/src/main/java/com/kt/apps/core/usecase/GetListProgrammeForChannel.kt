package com.kt.apps.core.usecase

import android.text.format.DateUtils
import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import io.reactivex.rxjava3.core.Observable
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@CoreScope
class GetListProgrammeForChannel @Inject constructor(
    private val parser: ParserExtensionsProgramSchedule
) : BaseUseCase<List<TVScheduler.Programme>>() {
    private val _programCacheHashMap by lazy {
        WeakHashMap<String, List<TVScheduler.Programme>>()
    }
    private val _lastGetListProgram by lazy {
        ConcurrentHashMap<String, Long>()
    }

    override fun prepareExecute(params: Map<String, Any>): Observable<List<TVScheduler.Programme>> {
        val channel = params[EXTRA_CHANNEL]
        val channelId: String? = when (channel) {
            is TVChannelDTO -> {
                channel.channelId
            }

            is ExtensionsChannel -> {
                channel.channelId
            }

            else -> {
                null
            }
        }
        val lastGetListProgram = _lastGetListProgram[channelId] ?: -1
        val cache = _programCacheHashMap[channelId]
        if (!channelId.isNullOrEmpty() && !cache.isNullOrEmpty() &&
            DateUtils.isToday(lastGetListProgram)
        ) {
            Logger.d(
                this, message = "Get cache for channel: $channelId," +
                        " lastGetListProgram: $lastGetListProgram," +
                        " cache: ${cache.size}"
            )
            return Observable.just(cache)
        }
        return when (channel) {
            is TVChannelDTO -> {
                val related = parser.getRelatedProgram(channel)
                if (related == null) {
                    parser.getListProgramForTVChannel(channel.channelId)
                } else {
                    parser.getListProgramForTVChannel(channel.channelId)
                        .mergeWith(related)
                        .reduce { t1, t2 ->
                            val t = t1.toMutableList()
                            t.addAll(t2)
                            t.distinctBy {
                                it.title + it.start
                            }.sortedBy {
                                it.startTimeMilli()
                            }
                        }.toObservable()
                }
            }

            is ExtensionsChannel -> {
                parser.getListProgramForExtensionsChannel(channel)
            }

            else -> {
                Observable.error<List<TVScheduler.Programme>>(Throwable("Not supported"))
            }
        }.doOnNext {
            if (!channelId.isNullOrEmpty() && it.isNotEmpty()) {
                _programCacheHashMap[channelId] = it
                _lastGetListProgram[channelId] = System.currentTimeMillis()
            }
        }
    }

    operator fun invoke(tvChannelDTO: TVChannelDTO) = execute(
        mapOf(
            EXTRA_CHANNEL to tvChannelDTO
        )
    )

    operator fun invoke(extensionsChannel: ExtensionsChannel) = execute(
        mapOf(
            EXTRA_CHANNEL to extensionsChannel
        )
    )

    companion object {
        private const val EXTRA_CHANNEL = "extra:channel"
    }

}
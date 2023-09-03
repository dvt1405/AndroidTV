package com.kt.apps.core.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetListProgrammeForChannel @Inject constructor(
    private val parser: ParserExtensionsProgramSchedule
) : BaseUseCase<List<TVScheduler.Programme>>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<List<TVScheduler.Programme>> {
        return when (val channel = params[EXTRA_CHANNEL]) {
            is TVChannelDTO -> {
                val related = parser.getRelatedProgram(channel)
                if (related == null) {
                    parser.getListProgramForTVChannel(channel)
                } else {
                    parser.getListProgramForTVChannel(channel)
                        .mergeWith(related)
                        .reduce { t1, t2 ->
                            val t = t1.toMutableList()
                            t.addAll(t2)
                            t.distinctBy {
                                it.title
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
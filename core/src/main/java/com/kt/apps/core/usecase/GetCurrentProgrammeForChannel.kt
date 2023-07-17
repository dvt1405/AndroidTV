package com.kt.apps.core.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.model.TVScheduler
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetCurrentProgrammeForChannel @Inject constructor(
    private val parser: ParserExtensionsProgramSchedule
) : BaseUseCase<TVScheduler.Programme>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<TVScheduler.Programme> {
        return when (val channel = params[EXTRA_CHANNEL]) {
            is String -> {
                parser.getCurrentProgramForTVChannel(channel)
                    .switchIfEmpty {
                        it.onError(Throwable("Empty program for channel id: $channel"))
                    }
            }

            is ExtensionsChannel -> {
                parser.getCurrentProgramForExtensionChannel(
                    channel, params[EXTRA_CHANNEL_TYPE] as? ExtensionsConfig.Type
                        ?: ExtensionsConfig.Type.MOVIE
                )
            }

            else -> {
                Observable.error<TVScheduler.Programme>(Throwable("Null params not supported"))
            }
        }
    }

    operator fun invoke(tvChannelId: String): Observable<TVScheduler.Programme> {
        return execute(
            mapOf(
                EXTRA_CHANNEL to tvChannelId
            )
        )
    }

    operator fun invoke(
        extensionsChannel: ExtensionsChannel,
        configType: ExtensionsConfig.Type
    ): Observable<TVScheduler.Programme> {
        return execute(
            mapOf(
                EXTRA_CHANNEL to extensionsChannel,
                EXTRA_CHANNEL_TYPE to configType
            )
        )
    }

    companion object {
        private const val EXTRA_CHANNEL = "extra:channel"
        private const val EXTRA_CHANNEL_TYPE = "extra:channel_type"
    }


}
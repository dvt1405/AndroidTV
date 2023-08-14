package com.kt.apps.core.tv.usecase

import com.kt.apps.core.logging.Logger
import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.tv.model.TVChannelLinkStream
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetChannelLinkStreamById @Inject constructor(
    private val getStreamLinkStreamFrom: GetTVChannelLinkStreamFrom,
    private val getTvLinks: GetListTVChannel
) : BaseUseCase<TVChannelLinkStream>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<TVChannelLinkStream> {
        val channelId = params["channelId"] as String
        Logger.d(
            this, message = "{" +
                    "channelId: $channelId}"
        )
        return getTvLinks()
            .retry(2)
            .flatMap { totalChannel ->
            try {
                Logger.d(
                    this, message = "{" +
                            "channelId: $channelId, " +
                            "totalChannel: $totalChannel }"
                )
                val chanel = totalChannel.first {
                    channelId == it.channelId
                }
                getStreamLinkStreamFrom(chanel)
                    .retry(2)
            } catch (e: NoSuchElementException) {
                Observable.error(ChannelNotFoundThrowable("Không tìm thấy kênh phù hợp!"))
            } catch (e: Exception) {
                Logger.e(this, exception = e)
                Observable.error(e)
            }
        }
    }
    class ChannelNotFoundThrowable(override val message: String) : Throwable(message)

    operator fun invoke(channelId: String): Observable<TVChannelLinkStream> = execute(
        mapOf(
            "channelId" to channelId
        )
    )


}
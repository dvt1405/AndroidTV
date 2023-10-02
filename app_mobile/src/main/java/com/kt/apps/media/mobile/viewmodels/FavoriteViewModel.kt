package com.kt.apps.media.mobile.viewmodels

import com.kt.apps.core.base.viewmodels.BaseFavoriteViewModel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.utils.TAG
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FavoriteViewModel@Inject constructor(
    private val _repository: IFavoriteRepository,
    private val roomDataBase: RoomDataBase
): BaseFavoriteViewModel(_repository) {

    suspend fun saveTVChannel(tvChannel: TVChannel) {
        return suspendCancellableCoroutine { cont ->
            add(
                _repository.save(
                    VideoFavoriteDTO(
                        id = tvChannel.channelId,
                        title = tvChannel.tvChannelName,
                        url = tvChannel.urls.first().url,
                        type = if (tvChannel.isRadio) {
                            VideoFavoriteDTO.Type.RADIO
                        } else {
                            VideoFavoriteDTO.Type.TV
                        },
                        category = tvChannel.tvGroup,
                        logoUrl = tvChannel.logoChannel,
                        sourceFrom = tvChannel.sourceFrom
                    )
                )
                    .doOnDispose { cont.cancel() }
                    .subscribe( {
                        Logger.d(TAG, "Save", "$tvChannel")
                        cont.resume(Unit)
                    }, { it ->
                        cont.resumeWithException(it)
                        Logger.e(TAG, "SaveError", it)
                    })
            )
        }
    }

    suspend fun unSaveTVChannel(tvChannel: TVChannel) {
        return suspendCancellableCoroutine<Unit> { cont ->
            add(
                _repository.delete(
                    VideoFavoriteDTO(
                        id = tvChannel.channelId,
                        title = tvChannel.tvChannelName,
                        url = tvChannel.urls.first().url,
                        type = VideoFavoriteDTO.Type.TV,
                        category = tvChannel.tvGroup,
                        logoUrl = tvChannel.logoChannel,
                        sourceFrom = tvChannel.sourceFrom
                    )
                )
                    .doOnDispose { cont.cancel() }
                    .subscribe( {
                        cont.resume(Unit)
                    }, { it ->
                        cont.resumeWithException(it)
                    })
            )
        }
    }
}
package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import com.kt.apps.core.base.viewmodels.BaseFavoriteViewModel
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.utils.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FavoriteViewModel@Inject constructor(
    private val _repository: IFavoriteRepository,
    private val roomDataBase: RoomDataBase
): BaseFavoriteViewModel(_repository) {

    suspend fun fetchList() {
        getListFavorite()
        listFavoriteLiveData.await(TAG)
    }
    suspend fun saveTVChannel(tvChannel: TVChannel) {
        Log.d(TAG, "saveTVChannel: init")
        return suspendCancellableCoroutine { cont ->
            Log.d(TAG, "saveTVChannel: execute")
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
                        Log.d(TAG, "saveTVChannel: complete")
                        cont.resume(Unit)
                    }, { it ->
                        cont.resumeWithException(it)
                        Logger.e(TAG, "SaveError", it)
                        Log.d(TAG, "saveTVChannel Error: ")
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

    suspend fun saveFavoriteKt(channel: ExtensionsChannel) {
        saveFavorite(channel)
        saveIptvChannelLiveData.await(tag = TAG)
    }

    suspend fun unsaveFavoriteKt(channel: ExtensionsChannel) {
        deleteFavorite(channel)
        deleteIptvChannelLiveData.await(TAG)
    }

    suspend fun getResultForItem(item: VideoFavoriteDTO) : ExtensionsChannelAndConfig? {
        return suspendCancellableCoroutine { cont ->
            add(
                _repository.get(item.id, VideoFavoriteDTO.Type.IPTV)
                    .flatMapSingle {
                        roomDataBase.extensionsChannelDao()
                            .getConfigAndChannelByStreamLink(item.url)
                    }
                    .doOnDispose { cont.cancel() }
                    .subscribe({
                        cont.resume(it)
                    }, {
                        cont.resumeWithException(it)
                    })
            )
        }
    }
}
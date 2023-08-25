package com.kt.apps.core.repository

import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dao.VideoFavoriteDAO
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@CoreScope
class FavoriteRepositoryImpl @Inject constructor(
    private val roomDataBase: RoomDataBase
) : IFavoriteRepository {
    private val favoriteDao: VideoFavoriteDAO by lazy { roomDataBase.videoFavoriteDao() }
    private val tvChannelDao by lazy {
        roomDataBase.tvChannelDao()
    }

    override fun getAll(): Maybe<List<VideoFavoriteDTO>> {
        return favoriteDao.getAll()
            .subscribeOn(Schedulers.io())

    }

    override fun save(item: VideoFavoriteDTO): Completable {
        return favoriteDao.save(item)
            .subscribeOn(Schedulers.io())

    }

    override fun saveIptv(iptv: ExtensionsChannel): Completable {
        val itemToInsert = VideoFavoriteDTO(
            id = iptv.channelId,
            url = iptv.tvStreamLink,
            title = iptv.tvChannelName,
            category = iptv.tvGroup,
            logoUrl = iptv.logoChannel,
            type = VideoFavoriteDTO.Type.IPTV,
            sourceFrom = iptv.sourceFrom
        )
        return favoriteDao.save(itemToInsert)
            .subscribeOn(Schedulers.io())

    }

    override fun saveTv(tv: TVChannelDTO): Completable {
        return tvChannelDao.getChannelWithUrl(tv.channelId)
            .map {
                VideoFavoriteDTO(
                    id = tv.channelId,
                    url = it.urls.first().url,
                    title = tv.tvChannelName,
                    category = tv.tvGroup,
                    logoUrl = tv.logoChannel,
                    type = VideoFavoriteDTO.Type.IPTV,
                    sourceFrom = tv.sourceFrom
                )
            }
            .subscribeOn(Schedulers.io())
            .switchMapCompletable {
                favoriteDao.save(it)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun get(id: String, type: VideoFavoriteDTO.Type): Maybe<VideoFavoriteDTO> {
        return favoriteDao.getById(id, type = type.name)
            .subscribeOn(Schedulers.io())
    }


    override fun delete(videoFavoriteDTO: VideoFavoriteDTO): Completable {
        return favoriteDao.delete(videoFavoriteDTO)
            .subscribeOn(Schedulers.io())

    }
}
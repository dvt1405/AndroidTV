package com.kt.apps.core.repository

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe

interface IFavoriteRepository {
    fun getAll(): Maybe<List<VideoFavoriteDTO>>

    fun save(item: VideoFavoriteDTO): Completable

    fun saveIptv(iptv: ExtensionsChannel): Completable

    fun saveTv(tv: TVChannelDTO): Completable

    fun get(id: String, type: VideoFavoriteDTO.Type): Maybe<VideoFavoriteDTO>

    fun delete(videoFavoriteDTO: VideoFavoriteDTO): Completable
}
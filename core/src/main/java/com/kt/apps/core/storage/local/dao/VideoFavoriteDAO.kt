package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe

@Dao
abstract class VideoFavoriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(videoFavoriteDAO: VideoFavoriteDTO): Completable

    @Delete
    abstract fun delete(videoFavoriteDAO: VideoFavoriteDTO): Completable

    @Transaction
    @Query("SELECT * FROM VIDEOFAVORITEDTO")
    abstract fun getAll(): Maybe<List<VideoFavoriteDTO>>

    @Transaction
    @Query("SELECT * FROM VIDEOFAVORITEDTO where id=:id AND type=:type")
    abstract fun getById(id: String, type: String): Maybe<VideoFavoriteDTO>

    @Transaction
    @Query("SELECT * FROM VIDEOFAVORITEDTO where id=:id AND type=:type AND url=:url")
    abstract fun getByIdAndUrl(id: String, type: String, url: String): Maybe<VideoFavoriteDTO>

    @Query("DELETE FROM VideoFavoriteDTO WHERE sourceFrom=:sourceId")
    abstract fun deleteBySourceId(sourceId: String): Completable

}
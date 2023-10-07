package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

@Dao
abstract class HistoryMediaDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(mediaItem: HistoryMediaItemDTO): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<HistoryMediaItemDTO>): Completable

    @Update
    abstract fun update(mediaItem: HistoryMediaItemDTO): Completable

    @Delete
    abstract fun delete(mediaItem: HistoryMediaItemDTO): Completable

    @Query("SELECT * from HistoryMediaItemDTO order by lastViewTime desc")
    @Transaction
    abstract fun getAll(): Single<List<HistoryMediaItemDTO>>

    @Query("SELECT itemId from HistoryMediaItemDTO")
    abstract fun getAllItemId(): Single<List<String>>

    @Query("SELECT * from HistoryMediaItemDTO WHERE itemId = :itemId")
    @Transaction
    abstract fun getItem(itemId: String): Single<HistoryMediaItemDTO>


    @Query("SELECT * from HistoryMediaItemDTO WHERE itemId = :itemId AND linkPlay = :streamLink")
    @Transaction
    abstract fun getItemEqualStreamLink(itemId: String, streamLink: String): Single<HistoryMediaItemDTO>
}
package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class ExtensionsChannelDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<ExtensionsChannel>): Completable

    @Update
    abstract fun update(list: List<ExtensionsChannel>): Completable

    @Delete
    abstract fun delete(channel: ExtensionsChannel): Completable

    @Delete
    abstract fun delete(listChannel: List<ExtensionsChannel>): Completable

    @Transaction
    @Query("DELETE FROM ExtensionsChannel WHERE extensionSourceId=:sourceId")
    abstract fun deleteBySourceId(sourceId: String): Completable

    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelDatabaseViews WHERE extensionSourceId=:sourceId "
    )
    abstract fun getAllBySourceId(sourceId: String): Single<List<ExtensionsChannel>>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelDatabaseViews WHERE tvStreamLink=:streamLink "
    )
    abstract fun getChannelByStreamLink(streamLink: String): Single<ExtensionsChannel>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelDatabaseViews " +
                "INNER JOIN ExtensionsConfig ON extensionSourceId=sourceUrl " +
                "WHERE tvGroup=:category " +
                "AND channelId=:itemId AND tvChannelName=:displayName "
    )
    abstract fun getConfigAndChannelByIdAndCategory(
        category: String,
        itemId: String,
        displayName: String
    ): Single<ExtensionsChannelAndConfig>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelDatabaseViews " +
                "INNER JOIN ExtensionsConfig ON extensionSourceId=sourceUrl " +
                "AND channelId=:itemId AND tvChannelName=:displayName "
    )
    abstract fun getConfigAndChannelByItemId(
        itemId: String,
        displayName: String
    ): Single<ExtensionsChannelAndConfig>
    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelDatabaseViews " +
                "INNER JOIN ExtensionsConfig ON extensionSourceId=sourceUrl " +
                "WHERE tvStreamLink=:streamLink "
    )
    abstract fun getConfigAndChannelByStreamLink(streamLink: String): Single<ExtensionsChannelAndConfig>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionsChannel WHERE extensionSourceId=:sourceId " +
                "ORDER BY tvChannelName " +
                "AND tvGroup=:category " +
                "LIMIT :limit " +
                "OFFSET :offset "
    )
    abstract fun getAllBySourceIdAndGroup(
        sourceId: String,
        category: String,
        limit: Int,
        offset: Int,
    ): Single<List<ExtensionsChannel>>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionsChannel WHERE tvChannelName LIKE '%'|:queryName|'%'" +
                "LIMIT :limit " +
                "OFFSET :offset "
    )
    abstract fun getAllByName(
        queryName: String,
        limit: Int,
        offset: Int,
    ): Single<List<ExtensionsChannel>>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionsChannelDBWithCategoryViews " +
                "WHERE lower(tvChannelName) GLOB '*'||:queryName||'*' " +
                "OR lower(categoryName) GLOB '*'||:queryName||'*' " +
                "LIMIT :limit " +
                "OFFSET :offset "
    )
    abstract fun searchByName(
        queryName: String,
        limit: Int,
        offset: Int,
    ): Single<List<ExtensionsChannelDBWithCategoryViews>>

    @Transaction
    @RawQuery
    abstract fun searchByNameQuery(
        queryName: SupportSQLiteQuery
    ): Single<List<ExtensionsChannelDBWithCategoryViews>>

}
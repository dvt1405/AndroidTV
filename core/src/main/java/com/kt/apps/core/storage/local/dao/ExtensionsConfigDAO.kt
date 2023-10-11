package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ExtensionsConfigWithLoadedListChannel
import com.kt.apps.core.storage.local.dto.ExtensionsConfigWithListCategory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class ExtensionsConfigDAO {

    @Query("SELECT * FROM ExtensionsConfig")
    @Transaction
    abstract fun getAll(): Maybe<List<ExtensionsConfig>>


    @Query("SELECT * FROM ExtensionsConfig WHERE sourceName = :name")
    @Transaction
    abstract fun getExtensions(name: String): Observable<ExtensionsConfig>

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id")
    @Transaction
    abstract fun getExtensionById(id: String): Maybe<ExtensionsConfig>

    @Query("SELECT COUNT(*) FROM ExtensionsConfig WHERE sourceUrl = :id")
    @Transaction
    abstract fun checkExtensionById(id: String): Single<Int>

    @Update
    abstract fun update(extensionsConfig: ExtensionsConfig): Completable

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id")
    @Transaction
    abstract fun getExtensionChannelList(id: String): Single<ExtensionsConfigWithLoadedListChannel>

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id limit 1")
    @Transaction
    abstract fun getExtensionChannelWithCategory(id: String): Single<ExtensionsConfigWithListCategory>

    @Delete
    abstract fun delete(config: ExtensionsConfig): Completable

    @Delete
    abstract fun delete(configs: List<ExtensionsConfig>): Completable

    @Query("DELETE FROM ExtensionsConfig")
    abstract fun deleteAll(): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(config: ExtensionsConfig): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(vararg config: ExtensionsConfig): Completable
}
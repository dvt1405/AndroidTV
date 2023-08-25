package com.kt.apps.core.di

import com.kt.apps.core.repository.FavoriteRepositoryImpl
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageImpl
import dagger.Binds
import dagger.Module

@Module
abstract class CoreModule {

    @Binds
    @CoreScope
    abstract fun bindsKeyValueStorage(
        keyValueStorageImpl: KeyValueStorageImpl
    ): IKeyValueStorage

    @Binds
    @CoreScope
    abstract fun bindFavoriteRepo(
        favoriteRepo: FavoriteRepositoryImpl
    ): IFavoriteRepository

    companion object {
    }
}
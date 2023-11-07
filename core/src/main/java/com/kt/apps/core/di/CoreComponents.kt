package com.kt.apps.core.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.viewmodels.HistoryIteractors
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import com.kt.apps.core.usecase.search.SearchForText
import dagger.BindsInstance
import dagger.Component
import io.reactivex.rxjava3.disposables.DisposableContainer
import okhttp3.OkHttpClient
import javax.inject.Named

@Component(
    modules = [
        CoreModule::class,
        StorageModule::class,
        FirebaseModule::class,
        NetworkModule::class,
        ExoModule::class
    ]
)
@CoreScope
interface CoreComponents {
    fun disposableContainer(): DisposableContainer
    fun roomDatabase(): RoomDataBase
    fun firebaseRemoteConfig(): FirebaseRemoteConfig
    fun firebaseDatabase(): FirebaseDatabase
    @Named(FirebaseModule.FIREBASE_VIP)
    fun firebaseDatabaseVip(): FirebaseDatabase
    fun fireStore(): FirebaseFirestore
    fun firebaseAnalytics(): FirebaseAnalytics
    fun sharedPreferences(): SharedPreferences
    fun okHttpClient(): OkHttpClient
    fun coreApp(): CoreApp
    fun context(): Context

    fun exoPlayerManager(): ExoPlayerManagerMobile

    fun parserExtensionsSource(): ParserExtensionsSource
    fun parserExtensionsProgram(): ParserExtensionsProgramSchedule
    fun getCurrentProgrammeForChannel() : GetCurrentProgrammeForChannel
    fun getListProgrammeForChannel() : GetListProgrammeForChannel
    fun searchForText() : SearchForText
    fun historyManager(): IMediaHistoryRepository
    fun historyIteractors(): HistoryIteractors

    fun inject(scope: BaseViewModel)

    fun keyValueStorage(): IKeyValueStorage
    fun favoriteRepo(): IFavoriteRepository

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(context: CoreApp): Builder

        @BindsInstance
        fun context(context: Context): Builder
        fun storageModule(storageModule: StorageModule): Builder
        fun build(): CoreComponents
    }
}
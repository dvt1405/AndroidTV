package com.kt.apps.core.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class FirebaseModule {

    @Provides
    @CoreScope
    fun providesFirebaseDataBase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @CoreScope
    @Named(FIREBASE_VIP)
    fun providesFirebaseDataBaseVip(): FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://xemtv-e551b-vip.asia-southeast1.firebasedatabase.app/"
    )

    @Provides
    @CoreScope
    @Named(FIREBASE_DEBUG)
    fun providesFirebaseDatabaseDebug(): FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://xemtv-e551b-dev.asia-southeast1.firebasedatabase.app/"
    )

    @Provides
    @CoreScope
    fun providesFirebaseFireStore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @CoreScope
    fun providesRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    @Provides
    @CoreScope
    fun providesAnalytics(): FirebaseAnalytics = Firebase.analytics

    companion object {
        const val FIREBASE_VIP = "FirebaseDatabaseVip"
        const val FIREBASE_DEBUG = "FirebaseDatabaseDebug"
    }

}
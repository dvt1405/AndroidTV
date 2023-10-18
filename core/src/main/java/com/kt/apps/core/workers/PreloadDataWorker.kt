package com.kt.apps.core.workers

import android.content.Context
import android.util.Log
import android.util.LogPrinter
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.storage.KeyValueStorageImpl
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.workers.factory.ChildWorkerFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONObject
import javax.inject.Inject

class PreloadDataWorker(
    private val context: Context,
    private val params: WorkerParameters,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val roomDataBase: RoomDataBase,
    private val keyValueStorage: KeyValueStorageImpl
): Worker(context, params) {

    private val extensionsConfigDAO by lazy {
        roomDataBase.extensionsConfig()
    }
    data class IPTVPreloadData(
        val sourceName: String = "",
        val sourceUrl: String = ""
    )
    override fun doWork(): Result {
        val version = firebaseRemoteConfig.getDouble(IPTV_VERSION)
        val savedVersion = keyValueStorage.get(PRELOAD_VERSION, Int::class.java)

        if (version > savedVersion) {
            executeLoadData()
        } else {
            Log.d(TAG, "doWork: Ignore fetch data")
        }
        return Result.success()
    }

    private fun executeLoadData() {
        val version = firebaseRemoteConfig.getDouble(IPTV_VERSION)
        val value = firebaseRemoteConfig.getString(IPTV_CHANNEL_KEY)
        val gson = Gson()
        val listSrc = gson.fromJson<List<IPTVPreloadData>>(value, TypeToken.getParameterized(
            List::class.java,
            IPTVPreloadData::class.java
        ).type).map {
            ExtensionsConfig(
                sourceName = it.sourceName,
                sourceUrl =  it.sourceUrl
            )
        }
        Observable.just(listSrc)
            .flatMapIterable { it }
            .flatMapCompletable {
                extensionsConfigDAO.insert(it)
            }
            .blockingSubscribe()
        keyValueStorage.save(PRELOAD_VERSION, version.toInt())
    }

    class Factory @Inject constructor(
        private val firebaseRemoteConfig: FirebaseRemoteConfig,
        private val roomDataBase: RoomDataBase,
        private val keyValueStorage: KeyValueStorageImpl
    ): ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker {
            return PreloadDataWorker(appContext, params, firebaseRemoteConfig, roomDataBase, keyValueStorage)
        }

    }
    companion object {
        const val IPTV_CHANNEL_KEY = "default_iptv_channel"
        const val IPTV_VERSION = "default_iptv_version"
        const val PRELOAD_VERSION = "PRELOAD_VERSION"
    }
}
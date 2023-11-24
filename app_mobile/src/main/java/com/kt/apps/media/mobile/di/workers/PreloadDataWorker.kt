package com.kt.apps.media.mobile.di.workers

import android.content.Context
import android.database.Observable
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.TAG
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class PreloadDataWorker(private val context: Context, private val params: WorkerParameters) :
    Worker(context, params) {
    private val disposable by lazy {
        CompositeDisposable()
    }
    private val extensionConfigDAO by lazy {
        RoomDataBase.getInstance(context)
            .extensionsConfig()
    }

    override fun doWork(): Result {
        disposable.add(
            io.reactivex.rxjava3.core.Observable.just(mapData.entries)
                .flatMapIterable { it ->
                    it
                }.flatMapCompletable {
                    extensionConfigDAO.insert(
                        ExtensionsConfig(
                            sourceName = it.key,
                            sourceUrl = it.value
                        )
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d(TAG, "doWork: Completed")
                }, {
                    Log.e(TAG, "doWork: $it")
                })
        )
        return Result.success()
    }

    companion object {
        val listData = listOf(
        "https://vthanhtivi.pw",
        "https://gg.gg/coocaa",
        "https://gg.gg/bearlivetv",
        "https://gg.gg/PHAPTX5",
        "https://bit.ly/beartvplay",
        "https://cvmtv.site",
        "https://gg.gg/khangg",
        "https://s.id/bearlivetv",
        "https://s.id/nhamng",
        "https://antmedia.anttv.xyz",
        "https://bit.ly/vietteliptv",
        "https://gg.gg/Coban66",
        "https://gg.gg/SN-tv",
        "https://hqth.me/fptphimle",
        "https://hqth.me/tvhaypl",
        "https://hqth.me/tvhaypb",
        "https://s.id/phimiptv",
        "https://s.id/ziptvvn",
        "https://gg.gg/phimiptv",
        "https://hqth.me/tvhayphimle",
        "https://gg.gg/vn360sport",
        "https://gg.gg/90phuttv",
        "https://gg.gg/SN-90phut",
        "https://gg.gg/SN-thethao",
        "https://gg.gg/SN-thapcam"
        )

        val mapData: Map<String, String> = listData.mapIndexed { index,str ->
            val key = Uri.parse(
                str.replace("https://", "")
                    .replace("http://","")
            ).pathSegments.lastOrNull { t -> t.trim().isNotEmpty() } ?: "Unknown${index}"
            key to str
        }.toMap()
    }
}
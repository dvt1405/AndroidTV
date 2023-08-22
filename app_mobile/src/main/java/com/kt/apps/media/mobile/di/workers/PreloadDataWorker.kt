package com.kt.apps.media.mobile.di.workers

import android.content.Context
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
        val mapData = mapOf<String, String>(
            "90phut" to "http://gg.gg/SN-90phut",
            "film" to "https://gg.gg/films24",
            "Truy@n hinh FPTI" to "https://hqth.me/fptv1",
            "Truyen hinh FPT2" to "https://hqth.me/fptv2i",
            "Truyen hinh VPT" to "https://hqth.me/vnptli",
            "Truyen hinh Viettel" to "https://hath.me/viettelv1",
            "VietSimpleTV" to "https://hqth.me/vietstv",
            "KIPTV SPORT" to "https://hqth.me/kiptvs",
            "VthanhTV" to "http://vthanhtivi.pw",
            "Coocaa" to "http://hgth.me/coca",
            "Nguyen Kiet" to "https://hqth.me/ngkiettv1",
            "VietNgaTV" to "https://hoth.me/vietngatv",
            "Beartv" to "http://bit.ly/beartvplay",
            "PCRTV" to "http://hqth.me/pcripty",
            "BepTV" to "https://hqth.me/bepty",
            "360 Sport" to "http://hqth.me/90p",
            "Phim mien phi" to "https://hqth.me/phimfree",
            "Phim le" to "http://hqth.me/phimle",
            "Phim le tvhay" to "http://hqth.me/tvhayphimle",
            "Phim le fptplay" to "http://hqth.me/fptphimle",
            "Phim bo" to "http://hqth.me/phimbo",
            "ChinaTV" to "http://hqth.me/china-ty",
            "Quoc te" to "https://hqth.me/qttvl",
        )
    }
}


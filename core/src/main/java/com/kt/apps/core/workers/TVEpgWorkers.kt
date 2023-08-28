package com.kt.apps.core.workers

import android.content.Context
import android.text.format.DateUtils
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.HOUR_MILLIS
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class TVEpgWorkers(
    context: Context,
    inputParams: WorkerParameters
) : RxWorker(
    context, inputParams
) {
    private val keyValueStorage by lazy {
        (context.applicationContext as CoreApp)
            .coreComponents
            .keyValueStorage()
    }

    private val parserEpgRepo by lazy {
        (context.applicationContext as CoreApp)
            .coreComponents
            .parserExtensionsProgram()
    }

    override fun createWork(): Single<Result> {
        val lastUpdate = keyValueStorage.get(EXTRA_LAST_UPDATE_EPG, Long::class.java)
        if (DateUtils.isToday(lastUpdate)) {
            if (System.currentTimeMillis() - lastUpdate < HOUR_MILLIS) {
                return Single.just(Result.success())
            }
        }
        return Single.create<Result> {
            parserEpgRepo.appendParseForConfigTask(
                ExtensionsConfig(
                    "",
                    "",
                    ExtensionsConfig.Type.TV_CHANNEL
                ),
                inputData.getString(EXTRA_DEFAULT_URL)!!
            )
            parserEpgRepo.runPendingSource()
        }.subscribeOn(Schedulers.io())
            .doOnSuccess {
                keyValueStorage.save(EXTRA_LAST_UPDATE_EPG, System.currentTimeMillis())
            }
    }

    companion object {
        const val EXTRA_LAST_UPDATE_EPG = "extra:last_update_epg"
        const val EXTRA_DEFAULT_URL = "extra:default_url"
    }
}
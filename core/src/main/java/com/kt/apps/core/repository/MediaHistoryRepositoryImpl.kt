package com.kt.apps.core.repository

import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.di.StorageModule
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@CoreScope
class MediaHistoryRepositoryImpl @Inject constructor(
    private val roomDataBase: RoomDataBase,
    @Named(StorageModule.EXTRA_EXO_DISPOSABLE)
    private val disposable: DisposableContainer
) : IMediaHistoryRepository {
    private val _historyDao by lazy {
        roomDataBase.historyItemDao()
    }

    private val _pendingHistoryQueue by lazy {
        mutableMapOf<String, Disposable>()
    }

    override fun saveHistoryItem(item: HistoryMediaItemDTO, delayInSeconds: Long) {
        val insertSource = _historyDao.getItemEqualStreamLink(item.itemId, item.linkPlay)
            .delay(delayInSeconds, TimeUnit.SECONDS)
            .filter { oldItem ->
                oldItem.currentPosition > item.currentPosition
            }
            .onErrorReturnItem(item)
            .defaultIfEmpty(item)
            .flatMapCompletable {
                if (it.currentPosition == item.currentPosition) {
                    _historyDao.insert(it)
                } else {
                    Completable.complete()
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe({
                _pendingHistoryQueue.remove(item.itemId)
                Logger.d(
                    this@MediaHistoryRepositoryImpl,
                    "SaveItem",
                    "$item"
                )
            }, {
                _pendingHistoryQueue.remove(item.itemId)
                Logger.e(
                    this@MediaHistoryRepositoryImpl,
                    "SaveError",
                    "$item"
                )
                Logger.e(
                    this@MediaHistoryRepositoryImpl,
                    "SaveError",
                    exception = it
                )
            })
        _pendingHistoryQueue[item.itemId]?.dispose()
        _pendingHistoryQueue[item.itemId] = insertSource
        disposable.add(insertSource)
    }

    override fun getHistoryForItem(itemId: String): Single<HistoryMediaItemDTO> {
        return _historyDao.getItem(itemId)
            .subscribeOn(Schedulers.io())
    }

    override fun getHistoryForItem(itemId: String, linkPlay: String): Single<HistoryMediaItemDTO> {
        return _historyDao.getItemEqualStreamLink(itemId, linkPlay)
            .subscribeOn(Schedulers.io())
    }

    override fun getListHistory(): Single<List<HistoryMediaItemDTO>> {
        return _historyDao.getAll()
            .subscribeOn(Schedulers.io())
    }

}
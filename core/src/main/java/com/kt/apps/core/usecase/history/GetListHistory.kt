package com.kt.apps.core.usecase.history

import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject

class GetListHistory @Inject constructor(
    private val roomDataBase: RoomDataBase
) : MaybeUseCase<List<HistoryMediaItemDTO>>() {
    private val historyDao by lazy {
        roomDataBase.historyItemDao()
    }

    override fun prepareExecute(params: Map<String, Any>): Maybe<List<HistoryMediaItemDTO>> {
        return historyDao.getAll()
            .map {
                val newList = it.filter {
                    it.type != HistoryMediaItemDTO.Type.TV
                }
                newList
            }
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .toMaybe()
    }

    operator fun invoke() = execute(mapOf())

    companion object {
    }
}
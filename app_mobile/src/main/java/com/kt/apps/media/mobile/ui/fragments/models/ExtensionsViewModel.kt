package com.kt.apps.media.mobile.ui.fragments.models

import android.provider.ContactsContract.Data
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.viewmodels.BaseExtensionsViewModel
import com.kt.apps.core.base.viewmodels.HistoryIteractors
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.security.cert.Extension
import javax.inject.Inject

typealias ExtensionResult = Map<ExtensionsConfig, List<ExtensionsChannel>>
@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase,
    private val getCurrentProgrammeForChannel: GetCurrentProgrammeForChannel,
    private val getListProgrammeForChannel: GetListProgrammeForChannel,
    private val actionLogger: IActionLogger,
    private val storage: IKeyValueStorage,
    private val historyIteractors: HistoryIteractors
) : BaseExtensionsViewModel(
    parserExtensionsSource,
    roomDataBase,
    getCurrentProgrammeForChannel,
    getListProgrammeForChannel,
    actionLogger,
    storage,
    historyIteractors
) {
//    private val _extensionsConfigs = MutableStateFlow<List<ExtensionsConfig>>(emptyList())
//    val extensionsConfigs: StateFlow<List<ExtensionsConfig>>
//        get() = _extensionsConfigs
//
//    private val _perExtensionChannelData = MutableStateFlow<ExtensionResult>(emptyMap())
//    val perExtensionChannelData: StateFlow<ExtensionResult>
//        get() = _perExtensionChannelData
//
//    private val observableData: Observable<List<ExtensionsConfig>>
//        get() = roomDataBase.extensionsConfig()
//            .getAll()
//            .observeOn(Schedulers.io())
//            .subscribeOn(AndroidSchedulers.mainThread())
//            .toObservable()
//
//
//    fun loadExtensionData() {
//        compositeDisposable.add(
//            observableData.flatMapIterable { x -> x }
//                .flatMapMaybe {
//                    parserExtensionsSource.parseFromRemoteRx(it)
//                            .map {result ->
//                                Pair(it, result)
//                            }
//                }
//                .subscribe ({
//                    _perExtensionChannelData.value = mapOf(it.first to it.second)
//                    _extensionsConfigs.update {existList ->
//                        if (existList.contains(it.first)) return@update existList
//                        existList + arrayListOf(it.first)
//                    }
//                }, {
//
//                })
//        )
//    }
//
//    fun deleteExtension(sourceName: String) {
//        extensionsConfigs.value.find {
//            it.sourceName == sourceName
//        }?.run {
//            compositeDisposable.add(
//                roomDataBase.extensionsConfig().delete(this)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribeOn(Schedulers.io())
//                    .subscribe {
//                        Log.d(TAG, "deleteExtension: Delete succeed")
//                        _extensionsConfigs.update {
//                            it.filterNot {ex ->
//                                ex.sourceName == sourceName
//                            }
//                        }
//                    }
//            )
//        }
//    }

}

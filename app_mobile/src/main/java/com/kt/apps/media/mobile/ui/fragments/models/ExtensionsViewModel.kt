package com.kt.apps.media.mobile.ui.fragments.models

import android.provider.ContactsContract.Data
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
import com.kt.apps.media.mobile.utils.asDataStateFlow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.security.cert.Extension
import javax.inject.Inject

typealias ExtensionResult = Map<ExtensionsConfig, List<ExtensionsChannel>>
sealed class AddSourceState {
    data class StartLoad(val source: ExtensionsConfig): AddSourceState()
    data class Success(val source: ExtensionsConfig): AddSourceState()
    data class Error(val throwable: Throwable): AddSourceState()
    object IDLE: AddSourceState()
}
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
    private val viewModelJob = SupervisorJob()
    private var processingExtensionConfig: MutableSharedFlow<ExtensionsConfig?> = MutableSharedFlow()
    val addSourceState: Flow<AddSourceState>
        get() = addExtensionConfigLiveData.asDataStateFlow(tag = "ExtensionsViewModel")
            .combine(processingExtensionConfig) { dataState, processing ->
                Log.d(TAG, "CombineStatus: $dataState $processing")
                if (processing == null) {
                    return@combine AddSourceState.IDLE
                }
                when (dataState) {
                    is DataState.Loading -> AddSourceState.StartLoad(processing)
                    is DataState.Success -> AddSourceState.Success(dataState.data)
                    is DataState.Error -> AddSourceState.Error(dataState.throwable)
                    else -> AddSourceState.IDLE
                }
            }

    fun cacheProcessingSource(ex: ExtensionsConfig) {
        CoroutineScope(Dispatchers.Main + viewModelJob).launch {
            processingExtensionConfig.emit(ex)
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

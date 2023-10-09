package com.kt.apps.media.mobile.ui.fragments.models

import android.provider.ContactsContract.Data
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.viewmodels.BaseExtensionsViewModel
import com.kt.apps.core.base.viewmodels.HistoryIteractors
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.removeLastRefreshExtensions
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.di.AppScope
import com.kt.apps.media.mobile.utils.asDataStateFlow
import com.kt.apps.media.mobile.utils.asUpdateFlow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.cert.Extension
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    val extensionConfigsKt: StateFlow<List<ExtensionsConfig>> by lazy {
        totalExtensionsConfig.asUpdateFlow("IPTVViewModel")
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )
    }

    suspend fun removeExtensionConfig(extensionsConfig: ExtensionsConfig) {
//        storage.removeLastRefreshExtensions(extensionsConfig)

        return suspendCancellableCoroutine { cont ->
            add(
                roomDataBase.extensionsConfig()
                    .delete(extensionsConfig)
                    .doOnComplete {
                        this.loadAllListExtensionsChannelConfig(true)
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Logger.d(this, message = "remove complete")
                        cont.resume(Unit)
                    }, {
                        Logger.e(this, exception = it)
                        cont.resumeWithException(it)
                    })
            )
        }
    }
}

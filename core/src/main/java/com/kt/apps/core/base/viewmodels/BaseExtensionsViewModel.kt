package com.kt.apps.core.base.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logAddIPTVSource
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.removeLastRefreshExtensions
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

@CoreScope
open class BaseExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase,
    private val getCurrentProgrammeForChannel: GetCurrentProgrammeForChannel,
    private val getListProgrammeForChannel: GetListProgrammeForChannel,
    private val actionLogger: IActionLogger,
    private val storage: IKeyValueStorage
) : BaseViewModel() {

    private val _totalExtensionsConfig by lazy {
        MutableLiveData<DataState<List<ExtensionsConfig>>>()
    }

    val totalExtensionsConfig: LiveData<DataState<List<ExtensionsConfig>>>
        get() = _totalExtensionsConfig


    private val _extensionsChannelListCache by lazy {
        mutableMapOf<String, WeakReference<List<ExtensionsChannel>>>()
    }

    init {
        loadAllListExtensionsChannelConfig(true)
    }

    val channelListCache: Map<String, WeakReference<List<ExtensionsChannel>>>
        get() = _extensionsChannelListCache


    fun appendExtensionsCache(id: String, channelList: List<ExtensionsChannel>) {
        Logger.e(this, message = "id = $id")
        _extensionsChannelListCache[id] = WeakReference(channelList)
    }

    fun loadChannelForConfig(configId: String): LiveData<DataState<List<ExtensionsChannel>>> {
        if (_extensionsChannelListCache[configId] != null
            && !_extensionsChannelListCache[configId]!!.get().isNullOrEmpty()
        ) {
            return MutableLiveData(DataState.Success(_extensionsChannelListCache[configId]!!.get()!!))
        }
        val liveData = MutableLiveData<DataState<List<ExtensionsChannel>>>()
        liveData.postValue(DataState.Loading())
        add(
            roomDataBase.extensionsConfig()
                .getExtensionById(configId)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    parserExtensionsSource.parseFromRemoteRx(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .retry { t1, t2 ->
                    return@retry t1 < 3
                }
                .subscribe({ tvList ->
                    appendExtensionsCache(configId, tvList)
                    liveData.postValue(DataState.Success(tvList))
                }, {
                    liveData.postValue(DataState.Error(it))
                    Logger.e(this, exception = it)
                })
        )
        return liveData
    }

    private val _programmeForChannelLiveData by lazy {
        MutableLiveData<DataState<TVScheduler.Programme>>()
    }

    val programmeForChannelLiveData: LiveData<DataState<TVScheduler.Programme>>
        get() = _programmeForChannelLiveData

    fun loadProgramForChannel(
        channel: ExtensionsChannel,
        extensionsType: ExtensionsConfig.Type
    ) {
        add(
            getCurrentProgrammeForChannel.invoke(channel, extensionsType)
                .subscribe({
                    _programmeForChannelLiveData.postValue(DataState.Success(it))
                }, {
                    _programmeForChannelLiveData.postValue(DataState.Error(it))
                })
        )
    }

    fun loadAllListExtensionsChannelConfig(refreshCache: Boolean = false) {
        if (!refreshCache && _totalExtensionsConfig.value is DataState.Success) {
            _totalExtensionsConfig.postValue(_totalExtensionsConfig.value)
            return
        }

        _totalExtensionsConfig.postValue(DataState.Loading())
        add(
            roomDataBase.extensionsConfig()
                .getAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _totalExtensionsConfig.postValue(DataState.Success(it))
                    Logger.d(this@BaseExtensionsViewModel, message = "addExtensionsPage")
                }, {
                    _totalExtensionsConfig.postValue(DataState.Error(it))
                })
        )
    }

    fun deleteExtensionConfig(extensionsConfig: ExtensionsConfig) {
        storage.removeLastRefreshExtensions(extensionsConfig)
    }

    fun parseExtensionByID(extensionsID: String) {
        add(
            roomDataBase.extensionsConfig()
                .getExtensionById(extensionsID)
                .subscribeOn(Schedulers.io())
                .flatMapMaybe {
                    parserExtensionsSource.parseFromRemoteMaybe(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tvList ->
                    appendExtensionsCache(extensionsID, tvList)
                }, {
                    Logger.e(this, exception = it)
                })
        )
    }

    private fun logAddIPTVSource(config: ExtensionsConfig) {
        actionLogger.logAddIPTVSource(config.sourceUrl, config.sourceName)
    }

    private val _addExtensionConfigLiveData by lazy {
        MutableLiveData<DataState<ExtensionsConfig>>()
    }
    val addExtensionConfigLiveData: LiveData<DataState<ExtensionsConfig>>
        get() = _addExtensionConfigLiveData

    private var pendingIptvSource: ExtensionsConfig? = null
    fun addIPTVSource(extensionsConfig: ExtensionsConfig) {
        if (extensionsConfig.sourceUrl == pendingIptvSource?.sourceUrl &&
            _addExtensionConfigLiveData.value is DataState.Loading
        ) {
            return
        }
        _addExtensionConfigLiveData.value = DataState.Loading()
        pendingIptvSource = extensionsConfig

        add(
            parserExtensionsSource.parseFromRemoteRx(extensionsConfig)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapCompletable {
                    if (it.isNotEmpty()) {
                        roomDataBase.extensionsConfig()
                            .insert(extensionsConfig)
                    } else {
                        Completable.error(Throwable("Data empty"))
                    }
                }
                .subscribe({
                    if (pendingIptvSource?.sourceUrl != extensionsConfig.sourceUrl) {
                        return@subscribe
                    }
                    logAddIPTVSource(extensionsConfig)
                    loadAllListExtensionsChannelConfig(true)
                    _addExtensionConfigLiveData.postValue(DataState.Success(extensionsConfig))
                    Logger.e(this@BaseExtensionsViewModel, message = "addIPTVSource Success: $extensionsConfig")
                }, {
                    if (pendingIptvSource?.sourceUrl != extensionsConfig.sourceUrl) {
                        return@subscribe
                    }
                    _addExtensionConfigLiveData.postValue(DataState.Error(it))
                    Logger.e(this@BaseExtensionsViewModel, exception = it)
                })
        )
    }

    fun insertDefaultSource() {
        add(
            parserExtensionsSource.insertAll()
                .subscribe({
                    loadAllListExtensionsChannelConfig(true)
                    Logger.d(this@BaseExtensionsViewModel, message = "insertDefaultSourceSuccess")
                }, {
                    Logger.e(this@BaseExtensionsViewModel, exception = it)
                })
        )
    }
}
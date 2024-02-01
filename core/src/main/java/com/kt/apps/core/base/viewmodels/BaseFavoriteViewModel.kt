package com.kt.apps.core.base.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import io.reactivex.rxjava3.disposables.Disposable
import javax.inject.Inject

open class BaseFavoriteViewModel @Inject constructor(
    private val repository: IFavoriteRepository
) : BaseViewModel() {

    private val _listFavoriteLiveData by lazy {
        MutableLiveData<DataState<List<VideoFavoriteDTO>>>()
    }

    val listFavoriteLiveData: LiveData<DataState<List<VideoFavoriteDTO>>>
        get() = _listFavoriteLiveData

    fun getListFavorite() {
        if (_listFavoriteLiveData.value is DataState.Loading) {
            return
        }
        Logger.d(this@BaseFavoriteViewModel, message = "getListFavorite")
        _listFavoriteLiveData.postValue(DataState.Loading())
        add(
            repository.getAll().subscribe({
                _listFavoriteLiveData.postValue(DataState.Success(it))
                Logger.d(this@BaseFavoriteViewModel, tag = "ListFavorite", message = "$it")
            }, {
                _listFavoriteLiveData.postValue(DataState.Error(it))
                Logger.e(this@BaseFavoriteViewModel, tag = "ErrorListFavorite", exception = it)
            })
        )
    }

    protected val _saveIptvChannelLiveData by lazy {
        MutableLiveData<DataState<Any>>()
    }

    val saveIptvChannelLiveData: LiveData<DataState<Any>>
        get() = _saveIptvChannelLiveData

    private var saveTask: Disposable? = null
    fun saveFavorite(iptvChannel: ExtensionsChannel) {
        saveTask?.dispose()
        saveTask = repository.saveIptv(iptvChannel)
            .subscribe({
                _saveIptvChannelLiveData.postValue(DataState.Success(iptvChannel))
                onShowFavouriteToMain()
                Logger.d(this@BaseFavoriteViewModel, tag = "SaveFavoriteSuccess", message = "$iptvChannel")
            }, {
                _saveIptvChannelLiveData.postValue(DataState.Error(it))
                getListFavorite()
                Logger.e(this@BaseFavoriteViewModel, tag = "SaveFavoriteError", exception = it)
            })
        add(saveTask!!)
    }

    open fun onShowFavouriteToMain() {

    }

    fun saveFavorite(tvChannelDTO: TVChannelDTO) {
        saveTask?.dispose()
        saveTask = repository.saveTv(tvChannelDTO)
            .subscribe({
                _saveIptvChannelLiveData.postValue(DataState.Success(tvChannelDTO))
                Logger.d(this@BaseFavoriteViewModel, tag = "SaveFavoriteSuccess", message = "$tvChannelDTO")
            }, {
                _saveIptvChannelLiveData.postValue(DataState.Error(it))
                getListFavorite()
                Logger.e(this@BaseFavoriteViewModel, tag = "SaveFavoriteError", exception = it)
            })
        add(saveTask!!)
    }

    protected val _deleteIptvChannelLiveData by lazy {
        MutableLiveData<DataState<Any>>()
    }

    val deleteIptvChannelLiveData: LiveData<DataState<Any>>
        get() = _deleteIptvChannelLiveData

    private var deleteTask: Disposable? = null
    fun deleteFavorite(iptvChannel: ExtensionsChannel) {
        deleteTask?.dispose()
        deleteTask = repository.delete(VideoFavoriteDTO.fromIPTVChannel(iptvChannel))
            .subscribe({
                _deleteIptvChannelLiveData.postValue(DataState.Success(iptvChannel))
                getListFavorite()
                onShowFavouriteToMain()
                Logger.d(this@BaseFavoriteViewModel, tag = "DeleteFavorite", message = "$iptvChannel")
            }, {
                _deleteIptvChannelLiveData.postValue(DataState.Error(it))
                Logger.e(this@BaseFavoriteViewModel, tag = "ErrorDeleteFavorite", exception = it)
            })

        add(deleteTask!!)
    }

}
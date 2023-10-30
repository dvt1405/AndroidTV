package com.kt.apps.media.xemtv.ui.favorite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.viewmodels.BaseFavoriteViewModel
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IFavoriteRepository
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import io.reactivex.rxjava3.disposables.Disposable
import javax.inject.Inject

class FavoriteViewModel @Inject constructor(
    private val _repository: IFavoriteRepository,
    private val roomDataBase: RoomDataBase,
    private val getChannelLinkStreamById: GetChannelLinkStreamById
) : BaseFavoriteViewModel(_repository) {

    fun saveTVChannel(tvChannel: TVChannel) {
        add(
            _repository.save(
                VideoFavoriteDTO(
                    id = tvChannel.channelId,
                    title = tvChannel.tvChannelName,
                    url = tvChannel.urls.first().url,
                    type = if (tvChannel.isRadio) {
                        VideoFavoriteDTO.Type.RADIO
                    } else {
                        VideoFavoriteDTO.Type.TV
                    },
                    category = tvChannel.tvGroup,
                    logoUrl = tvChannel.logoChannel,
                    sourceFrom = tvChannel.sourceFrom
                )
            ).subscribe({
                _saveIptvChannelLiveData.postValue(DataState.Success(tvChannel))
                Logger.d(this@FavoriteViewModel, "Save", "$tvChannel")
            }, {
                _saveIptvChannelLiveData.postValue(DataState.Error(it))
                Logger.e(this@FavoriteViewModel, "SaveError", it)
            })
        )
    }

    private var resultForItemTask: Disposable? = null
    private val _selectedItem by lazy {
        MutableLiveData<DataState<ExtensionsChannelAndConfig>>()
    }
    val selectedItem: LiveData<DataState<ExtensionsChannelAndConfig>>
        get() = _selectedItem

    fun getResultForItem(item: VideoFavoriteDTO) {
        resultForItemTask?.dispose()
        when (item.type) {
            VideoFavoriteDTO.Type.TV -> {

            }

            VideoFavoriteDTO.Type.IPTV -> {
                resultForItemTask = _repository.get(item.id, VideoFavoriteDTO.Type.IPTV)
                    .flatMapSingle {
                        roomDataBase.extensionsChannelDao()
                            .getConfigAndChannelByStreamLink(item.url)
                    }
                    .subscribe({
                        _selectedItem.postValue(DataState.Success(it))
                        Logger.d(this@FavoriteViewModel, "GetResultSuccess", "$it")
                    }, {
                        Logger.e(this@FavoriteViewModel, "GetResultError", it)
                    })
            }

            VideoFavoriteDTO.Type.RADIO -> {

            }
        }
        resultForItemTask?.let { add(it) }
    }

    fun deleteFavoriteTvChannel(tvChannel: TVChannel) {
        add(
            _repository.delete(
                VideoFavoriteDTO(
                    id = tvChannel.channelId,
                    title = tvChannel.tvChannelName,
                    url = tvChannel.urls.first().url,
                    type = VideoFavoriteDTO.Type.TV,
                    category = tvChannel.tvGroup,
                    logoUrl = tvChannel.logoChannel,
                    sourceFrom = tvChannel.sourceFrom
                )
            ).subscribe({
                _deleteIptvChannelLiveData.postValue(DataState.Success(tvChannel))
                Logger.d(this@FavoriteViewModel, "Delete", "$tvChannel")
            }, {
                _deleteIptvChannelLiveData.postValue(DataState.Error(it))
                Logger.e(this@FavoriteViewModel, "DeleteError", it)
            })
        )
    }

    fun clearLastSelectedStreamingTask() {
        resultForItemTask?.dispose()
        _selectedItem.postValue(DataState.None())
    }
}
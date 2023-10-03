package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.viewmodels.FavoriteViewModel
import kotlinx.coroutines.flow.StateFlow

interface IFavoriteControl {
    val favoriteViewModel: FavoriteViewModel
    val currentPlayingVideo: StateFlow<StreamLinkData?>
}

suspend fun IFavoriteControl.toggleFavoriteCurrent(isFavorite: Boolean) {
    val streamLinkData = currentPlayingVideo.value
    if (streamLinkData is StreamLinkData.TVStreamLinkData) {
        val currentVideo = streamLinkData.data
        if (isFavorite) {
            favoriteViewModel.unSaveTVChannel(currentVideo.channel)
        } else {
            favoriteViewModel.saveTVChannel(currentVideo.channel)
        }
    } else if (streamLinkData is StreamLinkData.ExtensionStreamLinkData) {
        if (isFavorite) {
            favoriteViewModel.unsaveFavoriteKt(streamLinkData.data)
        } else {
            favoriteViewModel.saveFavoriteKt(streamLinkData.data)
        }
    }
    loadFavorite()
}

fun IFavoriteControl.loadFavorite() {
    favoriteViewModel.getListFavorite()
}
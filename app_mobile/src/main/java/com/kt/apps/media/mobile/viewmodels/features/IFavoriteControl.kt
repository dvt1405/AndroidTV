package com.kt.apps.media.mobile.viewmodels.features

import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.viewmodels.FavoriteViewModel
import kotlinx.coroutines.flow.StateFlow

interface IFavoriteControl {
    val favoriteViewModel: FavoriteViewModel
    val currentPlayingVideo: StateFlow<StreamLinkData?>
}

suspend fun  IFavoriteControl.toggleFavoriteCurrent(isFavorite: Boolean) {
    val currentVideo = (currentPlayingVideo.value as? StreamLinkData.TVStreamLinkData)?.data ?: return
    if (isFavorite) {
        favoriteViewModel.unSaveTVChannel(currentVideo.channel)
    } else {
        favoriteViewModel.saveTVChannel(currentVideo.channel)
    }
    loadFavorite()
}

fun IFavoriteControl.loadFavorite() {
    favoriteViewModel.getListFavorite()
}
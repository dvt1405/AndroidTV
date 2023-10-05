package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.storage.local.dao.VideoFavoriteDAO
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.asUpdateFlow
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.viewmodels.features.IFavoriteControl
import com.kt.apps.media.mobile.viewmodels.features.IFetchFavoriteControl
import com.kt.apps.media.mobile.viewmodels.features.IFetchTVChannelControl
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoriteInteractor(
    private val provider: ViewModelProvider,
    private val scope: CoroutineScope
): IUIControl, IFetchFavoriteControl {
    override val tvChannelViewModel: TVChannelViewModel by lazy {
        provider[TVChannelViewModel::class.java]
    }

    override val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    override val favoriteViewModel: FavoriteViewModel by lazy {
        provider[FavoriteViewModel::class.java]
    }

    val listFavorite: StateFlow<List<VideoFavoriteDTO>> by lazy {
        favoriteViewModel.listFavoriteLiveData.asUpdateFlow(tag = TAG)
            .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())
    }

    suspend fun loadFavorites() {
        favoriteViewModel.getListFavorite()
        favoriteViewModel.listFavoriteLiveData.await()
    }

    suspend fun openPlayback(item: IChannelElement) {
        val item = (item as? ChannelElement.FavoriteVideo) ?: return
        loadFavoriteChannel(item)
    }
}


package com.kt.apps.media.mobile.viewmodels.features

import android.net.Uri
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.viewmodels.FavoriteViewModel

interface IFetchDataControl {
    val playbackViewModel: PlaybackViewModel
}
interface IFetchTVChannelControl: IFetchDataControl {
    val tvChannelViewModel: TVChannelViewModel
    suspend fun loadLinkStreamAction(
        element: ChannelElement.TVChannelElement,
        mapPrepareValue: (ChannelElement.TVChannelElement) -> PrepareStreamLinkData,
        mapSuccessValue: (TVChannelLinkStream) -> StreamLinkData
    ) {
        playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
        playbackViewModel.changeProcessState(
            PlaybackViewModel.State.LOADING(mapPrepareValue(element))
        )
        tvChannelViewModel.loadLinkStreamForChannel(element.model)
        val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await(tag = "TV")
        val data = mapSuccessValue(linkStream)
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
    }

    suspend fun retryGetLinkStreamAction() {
        tvChannelViewModel.retryGetLastWatchedChannel()
        val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await(tag = "TV")
        val data = StreamLinkData.TVStreamLinkData(linkStream)
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
    }

    fun loadProgramForChannel(element: ChannelElement.TVChannelElement) {
        tvChannelViewModel.getListProgramForChannel(element.model)
    }
}

interface IFetchFootballMatchControl: IFetchDataControl {
    val footballViewModel: FootballViewModel
}

interface IFetchRadioChannel: IFetchTVChannelControl

interface IFetchDeepLinkControl {
    val playbackViewModel: PlaybackViewModel
    val tvChannelViewModel: TVChannelViewModel
    val uiControlViewModel: UIControlViewModel
}

interface IFetchFavoriteControl: IFetchTVChannelControl, IUIControl {
    val favoriteViewModel: FavoriteViewModel
    suspend fun loadFavoriteChannel(element: ChannelElement.FavoriteVideo) {

        when (element.model.type) {
            VideoFavoriteDTO.Type.TV, VideoFavoriteDTO.Type.RADIO -> {
                playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
                tvChannelViewModel.getLinkStreamById(element.model.id)
                val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await(tag = "IFetchTVChannelControl")
                val prepare = if (linkStream.channel.isRadio) {
                    PrepareStreamLinkData.Radio(linkStream.channel)
                } else {
                    PrepareStreamLinkData.TV(linkStream.channel)
                }
                openPlayback(prepare)
            }
            VideoFavoriteDTO.Type.IPTV -> {
                val extensionsChannelAndConfig = favoriteViewModel.getResultForItem(element.model) ?: kotlin.run {
                    throw Throwable("Error")
                }
                openPlayback(PrepareStreamLinkData.IPTV(extensionsChannelAndConfig.channel, extensionsChannelAndConfig.config.sourceUrl))
            }
        }
    }
}

suspend fun IFetchTVChannelControl.loadLinkStreamChannel(element: ChannelElement.TVChannelElement) {
    loadLinkStreamAction(element,
        mapPrepareValue = {
        PrepareStreamLinkData.TV(it.model)
    },
        mapSuccessValue = {
        StreamLinkData.TVStreamLinkData(it)
    })
}



suspend fun IFetchDeepLinkControl.loadByDeepLink(deeplink: Uri) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    tvChannelViewModel.playTvByDeepLinks(deeplink)
    val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await()
    val streamLinkData = StreamLinkData.TVStreamLinkData(linkStream)
    val prepareStreamLinkData = if (linkStream.channel.isRadio) {
        PrepareStreamLinkData.Radio(linkStream.channel)
    } else {
        PrepareStreamLinkData.TV(linkStream.channel)
    }
    uiControlViewModel.openPlayback(prepareStreamLinkData)
    playbackViewModel.changeProcessState(
        PlaybackViewModel.State.LOADING(prepareStreamLinkData)
    )
    playbackViewModel.changeProcessState(
        PlaybackViewModel.State.PLAYING(streamLinkData)
    )
}

suspend fun IFetchRadioChannel.loadLinkStreamChannel(element: ChannelElement.TVChannelElement) {
    loadLinkStreamAction(element,
        mapPrepareValue = {
            PrepareStreamLinkData.Radio(it.model)
        },
        mapSuccessValue = {
            StreamLinkData.TVStreamLinkData(it)
        })
}

suspend fun IFetchFootballMatchControl.loadFootballMatchLinkStream(match: FootballMatch) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    playbackViewModel.changeProcessState(
        PlaybackViewModel.State.LOADING(PrepareStreamLinkData.Football(match))
    )
    footballViewModel.getLinkStreamFor(match)
    val linkStream = footballViewModel.footMatchDataState.await()
    val data = StreamLinkData.FootballStreamLinkData(match, linkStream)
    playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
}
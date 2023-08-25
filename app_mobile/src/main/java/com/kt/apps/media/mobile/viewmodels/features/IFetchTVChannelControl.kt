package com.kt.apps.media.mobile.viewmodels.features

import android.net.Uri
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.await

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
        val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await()
        val data = mapSuccessValue(linkStream)
        playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
    }

    suspend fun loadProgramForChannel(element: ChannelElement.TVChannelElement): TVScheduler.Programme {
        tvChannelViewModel.loadProgramForChannel(element.model)
        return tvChannelViewModel.programmeForChannelLiveData.await()
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
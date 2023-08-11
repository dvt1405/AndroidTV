package com.kt.apps.media.mobile.viewmodels.features

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
}

interface IFetchFootballMatchControl: IFetchDataControl {
    val footballViewModel: FootballViewModel
}

interface IFetchRadioChannel: IFetchTVChannelControl
suspend fun IFetchTVChannelControl.loadLinkStreamChannel(element: ChannelElement.TVChannelElement) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    playbackViewModel.changeProcessState(
        PlaybackViewModel.State.LOADING(PrepareStreamLinkData.TV(element.model))
    )
    tvChannelViewModel.loadLinkStreamForChannel(element.model)
    val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await()
    val data = StreamLinkData.TVStreamLinkData(linkStream)
    playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
}

suspend fun IFetchRadioChannel.loadLinkStreamChannel(element: ChannelElement.TVChannelElement) {
    playbackViewModel.changeProcessState(PlaybackViewModel.State.IDLE)
    playbackViewModel.changeProcessState(
        PlaybackViewModel.State.LOADING(PrepareStreamLinkData.Radio(element.model))
    )
    tvChannelViewModel.loadLinkStreamForChannel(element.model)
    val linkStream = tvChannelViewModel.tvWithLinkStreamLiveData.await()
    val data = StreamLinkData.TVStreamLinkData(linkStream)
    playbackViewModel.changeProcessState(PlaybackViewModel.State.PLAYING(data))
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
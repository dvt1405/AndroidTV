package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.isLiveMatch
import com.kt.apps.media.mobile.viewmodels.features.FootballViewModel
import com.kt.apps.media.mobile.viewmodels.features.IFetchFootballMatchControl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class FootballPlaybackInteractor(
    provider: ViewModelProvider,
    coroutineScope: LifecycleCoroutineScope
) : BasePlaybackInteractor(provider, coroutineScope), IFetchFootballMatchControl {
    override val footballViewModel: FootballViewModel by lazy {
        provider[FootballViewModel::class.java]
    }

    private val listMatches: StateFlow<List<FootballMatch>> by lazy {
        footballViewModel.listFootMatchDataState.asFlow()
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveMatches: StateFlow<List<FootballMatch>> by lazy {
        listMatches.mapLatest { list -> list.filter { it.isLiveMatch() } }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    }
}
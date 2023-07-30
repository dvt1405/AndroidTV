package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.isLiveMatch
import com.kt.apps.media.mobile.viewmodels.features.FootballViewModel
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import java.util.Calendar
import java.util.Locale

inline val sortRegex
    get() = Regex(".*?(c1|euro|epl|laliga|((?=.*?\\bpremier\\b)(?=.*?\\bleague\\b).*)|nba|uefa|european|((?=.*\\bngoại\\b)(?=.*\\bhạng\\b)(?=.*\\banh\\b).*)).*?")
class FootballListInteractor(private val provider: ViewModelProvider): IUIControl {
    private val footballViewModel: FootballViewModel by lazy {
        provider[FootballViewModel::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }
    private val listMatches: Flow<List<FootballMatch>>
        get() = footballViewModel.listFootMatchDataState.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val groupedMatches: Flow<Map<String, List<FootballMatch>>>
        get() = listMatches.mapLatest { list ->
            list.groupBy { it.league }
                .toSortedMap { o1, o2 ->
                    if (sortRegex.matches(o2.lowercase())) {
                        1
                    } else {
                        o1.compareTo(o2)
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveMatches: Flow<List<FootballMatch>>
        get() = listMatches.mapLatest { list ->
            list.filter { it.isLiveMatch() }
        }


    fun getAllMatches() {
        footballViewModel.getAllMatches()
    }

    suspend fun openPlayback(match: FootballMatch) {
        uiControlViewModel.openPlayback(PrepareStreamLinkData.Football(match))
    }

    fun getAllMatchesAsync(): Deferred<Unit> {
        return CoroutineScope(Dispatchers.Main).async {
            footballViewModel.getAllMatches()
            groupedMatches.first()
        }
    }




}
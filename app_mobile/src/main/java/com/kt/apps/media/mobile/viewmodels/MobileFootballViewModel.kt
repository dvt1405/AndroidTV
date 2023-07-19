package com.kt.apps.media.mobile.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.viewmodels.features.FootballViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import java.util.Calendar
import java.util.Locale

inline val sortRegex
    get() = Regex(".*?(c1|euro|epl|laliga|((?=.*?\\bpremier\\b)(?=.*?\\bleague\\b).*)|nba|uefa|european|((?=.*\\bngoại\\b)(?=.*\\bhạng\\b)(?=.*\\banh\\b).*)).*?")
class MobileFootballViewModel(private val provider: ViewModelProvider) {
    private val footballViewModel: FootballViewModel by lazy {
        provider[FootballViewModel::class.java]
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

    fun getAllMatchesAsync(): Deferred<Unit> {
        return CoroutineScope(Dispatchers.Main).async {
            footballViewModel.getAllMatches()
            groupedMatches.first()
        }
    }

    private fun FootballMatch.isLiveMatch(): Boolean {
        val calendar = Calendar.getInstance(Locale.TAIWAN)
        val currentTime = calendar.timeInMillis / 1000
        return  (currentTime - kickOffTimeInSecond) > -20 * 60
                && (currentTime - kickOffTimeInSecond) < 150 * 60
    }
}
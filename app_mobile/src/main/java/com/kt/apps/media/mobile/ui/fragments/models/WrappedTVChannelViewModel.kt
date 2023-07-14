package com.kt.apps.media.mobile.ui.fragments.models

import androidx.work.WorkManager
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.groupAndSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

typealias GroupTVChannel = Map<String,List<TVChannel>>
class WrappedTVChannelViewModel @Inject constructor(
    interactors: TVChannelInteractors,
    app: App,
    workManager: WorkManager
) : TVChannelViewModel(interactors, app, workManager) {
//
//    val groupTVChannel: Flow<GroupTVChannel>
//        get() = tvChannelLiveData.asFlow().map {
//            groupAndSort(it).associate { value ->
//                value.first to value.second
//            }
//        }
}
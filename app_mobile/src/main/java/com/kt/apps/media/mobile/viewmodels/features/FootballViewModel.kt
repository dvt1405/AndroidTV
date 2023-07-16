package com.kt.apps.media.mobile.viewmodels.features

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.Logger
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink
import com.kt.apps.football.usecase.GetLinkStreamByFootballTeam
import com.kt.apps.football.usecase.GetLinkStreamForFootballMatch
import com.kt.apps.football.usecase.GetListFootballMatch
import javax.inject.Inject

data class FootballInteractors @Inject constructor(
    val getListFootballMatch: GetListFootballMatch,
    val getLinkStreamForFootballMatch: GetLinkStreamForFootballMatch,
    val getLinkStreamByFootballTeam: GetLinkStreamByFootballTeam
)

class FootballViewModel @Inject constructor(
    private val interactors: FootballInteractors
) : BaseViewModel() {

    private val _listFootMatchDataState by lazy {
        MutableLiveData<DataState<List<FootballMatch>>>()
    }
    val listFootMatchDataState: LiveData<DataState<List<FootballMatch>>>
        get() = _listFootMatchDataState

    fun getAllMatches() {
        if (interactors.getListFootballMatch.cacheData != null) {
            _listFootMatchDataState.postValue(DataState.Success(interactors.getListFootballMatch.cacheData!!))
            return
        }
        _listFootMatchDataState.postValue(DataState.Loading())
        add(
            interactors.getListFootballMatch(FootballDataSourceFrom.Phut91)
                .subscribe({
                    _listFootMatchDataState.postValue(DataState.Success(it))
                    Logger.d(this@FootballViewModel, message = Gson().toJson(it))
                }, {
                    _listFootMatchDataState.postValue(DataState.Error(it))
                })
        )
    }


    private val _footMatchDataState by lazy {
        MutableLiveData<DataState<FootballMatchWithStreamLink>>()
    }
    val footMatchDataState: LiveData<DataState<FootballMatchWithStreamLink>>
        get() = _footMatchDataState

    fun getLinkStreamFor(footballMatch: FootballMatch) {
        _footMatchDataState.postValue(DataState.Loading())
        add(
            interactors.getLinkStreamForFootballMatch(footballMatch)
                .subscribe({
                    _footMatchDataState.postValue(DataState.Success(it))
                    Logger.d(this@FootballViewModel, message = Gson().toJson(it))
                }, {
                    _footMatchDataState.postValue(DataState.Error(it))
                })
        )
    }

    fun getLinkStreamBy(uri: Uri) {

    }

    fun streamFootballByDeepLinks(deepLink: Uri) {
        !(deepLink.host?.contentEquals(Constants.DEEPLINK_HOST) ?: return)
        val lastPath = deepLink.pathSegments.last() ?: return
        if (lastPath.contentEquals("xemtv")) return
        Logger.d(
            this, message = "play by deeplink: {" +
                    "uri: $deepLink" +
                    "}"
        )

        _footMatchDataState.postValue(DataState.Loading())
        add(
            interactors.getLinkStreamByFootballTeam(lastPath)
                .subscribe({
                    _footMatchDataState.postValue(DataState.Success(it))
                    Logger.d(this@FootballViewModel, message = Gson().toJson(it))
                }, {
                    Logger.e(this@FootballViewModel, exception = it)
                    _footMatchDataState.postValue(DataState.Error(it))
                })
        )

    }

    fun clearState() {
        _footMatchDataState.postValue(DataState.None())
    }


}
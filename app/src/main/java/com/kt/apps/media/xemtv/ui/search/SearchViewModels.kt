package com.kt.apps.media.xemtv.ui.search

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logSearchForText
import com.kt.apps.core.logging.logSearchForTextAndPerformClick
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.model.TVChannel.Companion.mapToTVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.usecase.search.SearchForText
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchViewModels @Inject constructor(
    private val roomDataBase: RoomDataBase,
    private val searchForText: SearchForText,
    private val actionLogger: IActionLogger
) : BaseViewModel() {

    private val _searchQueryLiveData by lazy {
        MutableLiveData<DataState<Map<String, List<SearchForText.SearchResult>>>>()
    }

    val searchQueryLiveData: LiveData<DataState<Map<String, List<SearchForText.SearchResult>>>>
        get() = _searchQueryLiveData

    var searchTask: Disposable? = null
    private var _lastSearchQuery: String? = null
    private val _logSearchQuery by lazy {
        LogSearchQuery("", -1, actionLogger)
    }
    val lastSearchQuery: String?
        get() = _lastSearchQuery
    fun queryDefaultSearch() {
        if (_lastSearchQuery.isNullOrBlank()) {
            querySearch("")
        }
    }

    fun querySearch(query: String?, filter: String? = null, page: Int = 0) {
        query ?: return
        if (_lastSearchQuery == query.trim() && _searchQueryLiveData.value is DataState.Loading) {
            return
        }
        _searchQueryLiveData.postValue(DataState.Loading())
        _lastSearchQuery = query
        searchTask?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }
        searchTask = searchForText(query, filter, limit = 1500, offset = page * 1500)
            .delay(300, TimeUnit.MILLISECONDS)
            .doOnDispose {
                mHandler.removeCallbacks(_logSearchQuery)
            }
            .doOnSuccess {
                mHandler.removeCallbacks(_logSearchQuery)
                _logSearchQuery.queryText = query
                _logSearchQuery.queryResult = it.values.takeIf {
                    it.isNotEmpty()
                }?.map {
                    it.size
                }?.reduce { acc, i ->
                    acc + i
                } ?: 0
                mHandler.postDelayed(_logSearchQuery, 2000)
            }
            .subscribe({
                _searchQueryLiveData.postValue(DataState.Success(it))
            }, {
                _searchQueryLiveData.postValue(DataState.Error(it))
            })
        add(searchTask!!)
    }

    private val _selectedItemLiveData by lazy {
        MutableLiveData<DataState<Any>>()
    }

    val selectedItemLiveData: LiveData<DataState<Any>>
        get() = _selectedItemLiveData

    private var getResulItemTask: Disposable? = null

    fun getResultForItem(
        searchItem: SearchForText.SearchResult,
        query: String,
    ) {
        actionLogger.logSearchForTextAndPerformClick(
            query = query,
            searchResult = searchItem
        )
        getResulItemTask?.dispose()
        getResulItemTask = when (searchItem) {
            is SearchForText.SearchResult.TV -> {
                _selectedItemLiveData.postValue(DataState.Loading())
                roomDataBase.tvChannelDao()
                    .getChannelWithUrl(searchItem.data.channelId)
                    .map {
                        TVChannelLinkStream(
                            it.mapToTVChannel(),
                            it.urls.map { it.url }
                        )
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        _selectedItemLiveData.postValue(DataState.Success(it))
                    }, {
                        Logger.e(this@SearchViewModels, "${it.message}", it)
                        _selectedItemLiveData.postValue(DataState.Error(it))
                    })
            }

            is SearchForText.SearchResult.ExtensionsChannelWithCategory -> {
                _selectedItemLiveData.postValue(DataState.Loading())
                roomDataBase.extensionsChannelDao()
                    .getConfigAndChannelByStreamLink(searchItem.data.tvStreamLink)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        _selectedItemLiveData.postValue(DataState.Success(it))
                    }, {
                        Logger.e(this@SearchViewModels, "${it.message}", it)
                        _selectedItemLiveData.postValue(DataState.Error(it))
                    })

            }

            is SearchForText.SearchResult.History -> {
                _selectedItemLiveData.postValue(DataState.Loading())
                roomDataBase.extensionsChannelDao()
                    .getConfigAndChannelByIdAndCategory(
                        searchItem.data.category,
                        searchItem.data.itemId,
                        searchItem.data.displayName
                    )
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        _selectedItemLiveData.postValue(DataState.Success(it))
                    }, {
                        Logger.e(this@SearchViewModels, "${it.message}", it)
                        _selectedItemLiveData.postValue(DataState.Error(it))
                    })

            }
        }
        add(getResulItemTask!!)
    }

    fun getDefaultSearchList() {
        if (searchTask != null && _searchQueryLiveData.value is DataState.Loading) {
            return
        }
        _searchQueryLiveData.postValue(DataState.Loading())
        searchTask?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }
        searchTask = searchForText()
            .subscribe({
                _searchQueryLiveData.postValue(DataState.Success(it))
            }, {
                _searchQueryLiveData.postValue(DataState.Error(it))
            })
        add(searchTask!!)
    }

    fun clearLastSelectedItem() {
        getResulItemTask?.dispose()
        _selectedItemLiveData.postValue(DataState.None())
    }

    class LogSearchQuery(
        var queryText: String,
        var queryResult: Int = 0,
        private val logger: IActionLogger
    ) : Runnable {
        override fun run() {
            if (queryText.isNotBlank() && queryResult > -1) {
                logger.logSearchForText(queryText, queryResult)
                queryText = ""
                queryResult = -1
            }
        }
    }

    private val mHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
            }
        }
    }
}
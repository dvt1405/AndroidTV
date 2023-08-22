package com.kt.apps.media.mobile.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

private const val HISTORY_LIST_KEY = "HISTORY_LIST_KEY"
private const val MAX_ITEM_HISTORY = 5
class SearchDashboardViewModel(private val provider: ViewModelProvider, private val context: Context) {

    private val searchViewModel by lazy {
        provider[SearchViewModels::class.java]
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val uiControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    val searchQueryData = uiControlViewModel.searchQuery

    val onOpenPlayback = uiControlViewModel.openPlayback

    suspend fun registerHistorySearchList(): Flow<List<String>> {
        return callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == HISTORY_LIST_KEY) {
                        trySend(sharedPreferences.listHistory())
                    }
                }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }
            .onStart { emit(sharedPreferences.listHistory()) }
    }

    fun saveHistorySearch(text: String) {
        sharedPreferences.edit(true) {
            val current = sharedPreferences.getString(HISTORY_LIST_KEY, "")
            val list = current?.topOf(MAX_ITEM_HISTORY) ?: emptyList()
            val mutableList = list.toMutableList()
            mutableList.removeAll { it == text }
            mutableList.add(text)
            putString(HISTORY_LIST_KEY, mutableList.joinToString(","))
        }
    }

    fun performSearch(string: String) {
        searchViewModel.querySearch(string)
    }

    fun performClearSearch() {
        searchViewModel.clearSearchList()
    }

    private fun String.topOf(value: Int): List<String> {
        return this.split(",").takeLast(value)
    }

    private fun SharedPreferences.listHistory(): List<String> {
        return getString(HISTORY_LIST_KEY, "")?.topOf(MAX_ITEM_HISTORY) ?: emptyList()
    }
}
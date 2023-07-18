package com.kt.apps.core.usecase.search

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.collect.MapMaker
import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.usecase.history.GetListHistory
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject

@CoreScope
class SearchForText @Inject constructor(
    private val roomDataBase: RoomDataBase,
    private val _getListHistory: GetListHistory
) : MaybeUseCase<Map<String, List<SearchForText.SearchResult>>>() {
    private val cacheResultValue: ConcurrentMap<String, List<SearchResult>> by lazy {
        MapMaker()
            .weakValues()
            .makeMap()
    }
    sealed class SearchResult(open val score: Int) {
        class ExtensionsChannelWithCategory(
            val data: ExtensionsChannelDBWithCategoryViews,
            val highlightTitle: SpannableString,
            override val score: Int,
        ) : SearchResult(score)

        data class TV(
            val data: TVChannelDTO,
            val urls: List<TVChannelDTO.TVChannelUrl>,
            val highlightTitle: SpannableString,
            override val score: Int,
            ) : SearchResult(score)

        data class History(
            val data: HistoryMediaItemDTO
        ) : SearchResult(0)
    }

    override fun prepareExecute(params: Map<String, Any>): Maybe<Map<String, List<SearchResult>>> {
        val defaultListItem = params[EXTRA_DEFAULT_HISTORY] as? Boolean ?: false
        val query = (params[EXTRA_QUERY] as? String ?: "").trim()
        val limit = params[EXTRA_LIMIT] as? Int ?: 1500
        val offset = params[EXTRA_OFFSET] as? Int ?: 0
        val filter = params[EXTRA_FILTER] as? String
        val cacheKey = query.lowercase().removeAllSpecialChars().trim()
        var queryNormalize = query.lowercase()
            .replaceVNCharsToLatinChars()
            .trim()

        while (queryNormalize.contains("  ")) {
            queryNormalize = queryNormalize.replace("  ", " ")
                .trim()
        }

        val filterHighlight = query.lowercase()
            .replaceVNCharsToLatinChars()
            .split(" ")
            .filter {
                it.isNotBlank() && it.isNotEmpty()
            }.flatMap {
                val unSpecialChar = it.removeAllSpecialChars()
                if (it != unSpecialChar) {
                    return@flatMap listOf(unSpecialChar, it)
                }
                return@flatMap listOf(it)
            }

        var searchQuery = query.lowercase().removeAllSpecialChars()
        while (searchQuery.contains("  ")) {
            searchQuery = searchQuery.replace("  ", " ")
                .trim()
        }

        val extraQuery = getExtraQuery(searchQuery, "searchKey", "", false)
        val rawQuery = if (searchQuery.isEmpty()) {
            "SELECT * FROM TVChannelFts4"
        } else {
            val q = searchQuery.replaceVNCharsToLatinChars()
                .removeAllSpecialChars()
            if (extraQuery.isNotEmpty()) {
                "SELECT * FROM TVChannelFts4 WHERE ${extraQuery.trim().removePrefix("OR")}" +
                        " OR searchKey LIKE '%$q%'" +
                        ""
            } else {
                "SELECT * FROM TVChannelFts4 WHERE searchKey LIKE '%$q%'"
            }
        }
        Logger.d(this@SearchForText, "TVChannel", rawQuery)
        val tvChannelSource: Single<List<SearchResult>> = roomDataBase.tvChannelDao()
            .searchChannelByNameFts4(SimpleSQLiteQuery(rawQuery))
            .onErrorReturnItem(listOf())
            .map {
                it.map {
                    SearchResult.TV(
                        it.tvChannel, it.urls,
                        getHighlightTitle(it.tvChannel.tvChannelName, filterHighlight),
                        calculateScore(it.tvChannel.tvChannelName, queryNormalize, filterHighlight)
                                + calculateScore(it.tvChannel.tvGroup, queryNormalize, filterHighlight)
                    )
                }
            }

        val extensionsSource: Single<List<SearchResult>> = roomDataBase.extensionsChannelDao()
            .searchByNameQuery(getSqlQuery(query, filter, limit, offset))
            .map {
                val list = it.map {
                    val calculateScore = calculateScore(it.tvChannelName, queryNormalize, filterHighlight)
                    val calculateScore2 = calculateScore(it.categoryName, queryNormalize, filterHighlight)
                    SearchResult.ExtensionsChannelWithCategory(
                        it,
                        getHighlightTitle(it.tvChannelName, filterHighlight),
                        calculateScore + calculateScore2
                    )
                }.sortedBy {
                    it.score
                }
                list
            }

        val historySource = _getListHistory.invoke()
            .filter {
                it.isNotEmpty()
            }
            .map { itemList ->
                val listSearchResult: List<SearchResult> = itemList.map { SearchResult.History(it) }
                listSearchResult
            }
            .switchIfEmpty(tvChannelSource)

        val totalSearchResult =  when {
            filter == FILTER_ONLY_TV_CHANNEL -> tvChannelSource.toFlowable()
            defaultListItem || searchQuery.isEmpty() -> historySource.toFlowable()
            filter == FILTER_ALL_IPTV -> extensionsSource.toFlowable()
            !filter?.trim().isNullOrBlank() -> {
                extensionsSource.toFlowable()
            }
            else -> tvChannelSource.mergeWith(extensionsSource)
                .reduce { t1, t2 ->
                    val l = t1.toMutableList()
                    l.addAll(t2)
                    l
                }.toFlowable()
        }.map {
            it.sortedBy {
                it.score
            }.groupBy {
                when (it) {
                    is SearchResult.History -> {
                        "Đã xem gần đây"
                    }

                    is SearchResult.TV -> {
                        it.data.tvGroup
                    }

                    is SearchResult.ExtensionsChannelWithCategory -> {
                        it.data.categoryName
                    }
                }
            }
        }.reduce { t1, t2 ->
            val list: MutableMap<String, List<SearchResult>> = t1.toMutableMap()
            list.putAll(t2)
            list
        }.doOnSuccess {
            synchronized(cacheResultValue) {
                val keyCache = query.lowercase().removeAllSpecialChars().trim()
                cacheResultValue[keyCache] = it.values.flatten()
            }
        }

        return if (!cacheResultValue[cacheKey].isNullOrEmpty()) {
            getResultFromCache(cacheKey)
                .onErrorResumeNext {
                    return@onErrorResumeNext totalSearchResult
                }
        } else {
            totalSearchResult
        }
    }

    private fun getResultFromCache(cacheKey: String) = Maybe.just(cacheResultValue[cacheKey]!!.groupBy {
        when (it) {
            is SearchResult.TV -> {
                it.data.tvGroup
            }

            is SearchResult.ExtensionsChannelWithCategory -> {
                it.data.categoryName
            }

            is SearchResult.History -> {
                it.data.category
            }
        }
    })

    operator fun invoke(
        query: String,
        filter: String?,
        limit: Int,
        offset: Int
    ) = execute(
        mapOf(
            EXTRA_QUERY to query.lowercase(),
            EXTRA_LIMIT to limit,
            EXTRA_OFFSET to offset,
            EXTRA_FILTER to (filter ?: "")
        )
    )

    operator fun invoke() = execute(
        mapOf(
            EXTRA_DEFAULT_HISTORY to true
        )
    )

    private fun getSqlQuery(
        queryString: String,
        filter: String?,
        limit: Int,
        offset: Int
    ): SupportSQLiteQuery {
        val extraQuery = getExtraQuery(queryString, "tvChannelName", "categoryName")
        var filterById = ""
        if (!filter?.trim().isNullOrBlank() && filter != FILTER_ALL_IPTV) {
            filterById = " AND configSourceUrl='$filter'"
        }
        val queryStr = "SELECT extensionSourceId as configSourceUrl, tvGroup as categoryName, tvChannelName, " +
                "logoChannel, tvStreamLink, sourceFrom FROM ExtensionsChannelFts4 " +
                "WHERE (tvChannelName MATCH '*$queryString*' OR categoryName MATCH '*$queryString*'$extraQuery)$filterById " +
                "LIMIT $limit " +
                "OFFSET $offset"

        Logger.d(
            this, message = "Query: {" +
                    "queryStr: $queryStr" +
                    "}"
        )
        return SimpleSQLiteQuery(queryStr)
    }

    private fun getExtraQuery(
        queryString: String,
        channelNameField: String,
        categoryField: String,
        useFts4: Boolean = true
    ): String {
        val splitStr = queryString.lowercase().split(" ")
            .filter {
                it.isNotBlank()
            }.flatMap {
                val unSpecialChar = it.removeAllSpecialChars()
                if (it != unSpecialChar) {
                    return@flatMap listOf(it, unSpecialChar)
                }
                return@flatMap listOf(it)
            }
        var regexSplit = ""
        if (splitStr.size > 1) {
            regexSplit = splitStr.mapIndexed { index, s ->
                val match = if (useFts4) {
                    if (index == splitStr.size - 1 || index == 0) {
                        "MATCH '*$s*'"
                    } else {
                        "MATCH '*$s *'"
                    }
                } else {
                    "LIKE '%$s%'"
                }
                val filterCategory = if (categoryField.isNotBlank()) {
                    "OR $categoryField $match"
                } else {
                    ""
                }
                val filterName = " OR $channelNameField $match"
                return@mapIndexed "$filterName$filterCategory"
            }.reduce { acc, s ->
                if (acc.contains(s)) {
                    acc
                } else {
                    "$acc$s"
                }
            }
        }
        return regexSplit
    }

    companion object {
        private const val EXTRA_QUERY = "extra:query"
        private const val EXTRA_LIMIT = "extra:limit"
        private const val EXTRA_OFFSET = "extra:offset"
        private const val EXTRA_FILTER = "extra:filter"
        private const val EXTRA_DEFAULT_HISTORY = "extra:default_history"

        const val FILTER_ONLY_TV_CHANNEL = "tv"
        const val FILTER_ALL_IPTV = "all_iptv"
        const val FILTER_FOOT_BALL = "football"

        private val HIGH_LIGHT_COLOR by lazy {
            Color.parseColor("#fb8500")
        }
        private val FOREGROUND_HIGH_LIGHT_COLOR by lazy {
            ForegroundColorSpan(HIGH_LIGHT_COLOR)
        }

        fun getHighlightTitle(realTitle: String, _filterHighlight: List<String>?): SpannableString {
            val spannableString = SpannableString(realTitle.trim())
            val lowerRealTitle = realTitle.trim()
                .lowercase()
                .replaceVNCharsToLatinChars()
            if (_filterHighlight.isNullOrEmpty()) {
                return spannableString
            }
            _filterHighlight.filter {
                it.isNotBlank() && it.trim().isNotEmpty()
            }.forEach { searchKey ->
                val listSpan = findSpan(lowerRealTitle, searchKey)
                if (listSpan.isNotEmpty()) {
                    for ((startIndex, endIndex) in listSpan) {
                        spannableString.setSpan(
                            ForegroundColorSpan(HIGH_LIGHT_COLOR),
                            startIndex,
                            endIndex + 1,
                            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                        )
                    }
                }
            }
            return spannableString
        }

        private fun findSpan(lowerRealTitle: String, searchKey: String): List<Pair<Int, Int>> {
            var start = 0
            var end = -1
            val listSpan = mutableListOf<Pair<Int, Int>>()
            var scanIndex = 0
            for (i in lowerRealTitle.indices) {
                val ch = lowerRealTitle[i]
                if (ch == searchKey[scanIndex]) {
                    if (scanIndex == 0) {
                        start = i
                    }
                    if (scanIndex == searchKey.length - 1) {
                        end = i
                    }
                    scanIndex++
                } else if (ch !in '0'..'9'
                    && ch !in 'a'..'z'
                    && ch != '+'
                    && ch != ' '
                ) {
                    continue
                } else if (ch == ' ') {
                    scanIndex = 0
                    start = 0
                    end = -1
                } else if (scanIndex > 0) {
                    if (ch == searchKey[0]) {
                        start = i
                        scanIndex = 1
                        end = -1
                    } else {
                        scanIndex = 0
                        start = 0
                        end = -1
                    }
                }
                if (scanIndex >= searchKey.length) {
                    if (end != -1) {
                        listSpan.add(Pair(start, end))
                    }
                    start = 0
                    end = -1
                    scanIndex = 0
                }
            }
            return listSpan
        }

        fun calculateScore(text: String, queryNormalize: String, pattern: List<String>): Int {
            var score = INIT_SCORE
            val textNormalize = text.trim().lowercase()
                .replaceVNCharsToLatinChars()
            if (textNormalize.equals(queryNormalize, ignoreCase = true)) {
                return 0
            }
            if (textNormalize.equals(queryNormalize.removeAllSpecialChars(), ignoreCase = true)) {
                return 1
            }

            val wordArrLatin = text.trim().lowercase()
                .replaceVNCharsToLatinChars()
                .split(" ")
                .filter {
                    it.isNotBlank()
                }

            if (wordArrLatin.contains(queryNormalize)) {
                score -= pattern.size
            }

            var child = ""
            for (i in pattern.indices) {
                val childPattern = pattern[i]
                if (i == 0) {
                    child = childPattern
                } else {
                    child += " $childPattern"
                }
                var index = textNormalize.indexOf(child)
                while (index > -1 && index + child.length < textNormalize.length) {
                    score -= if (index == 0 && i == 0) {
                        3
                    } else {
                        1
                    }
                    index = textNormalize.indexOf(child, index + child.length)
                }
                for (j in wordArrLatin.indices) {
                    if (wordArrLatin[j] == childPattern) {
                        score--
                        if (j == i) {
                            score -= 2
                        }
                    }
                }
            }

            if (wordArrLatin.size == pattern.size) {
                score--
            }

            return score
        }

        private const val INIT_SCORE = 100
    }

}
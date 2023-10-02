package com.kt.apps.football.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.model.FootballMatch
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetListFootballMatch @Inject constructor(
    private val sourceIterator: Map<FootballDataSourceFrom, @JvmSuppressWildcards IFootballMatchDataSource>
) : BaseUseCase<List<FootballMatch>>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<List<FootballMatch>> {
        val sourceFrom = params[EXTRA_SOURCE_FROM] as FootballDataSourceFrom
        val repo = sourceIterator[sourceFrom]
        return repo!!.getAllMatches()
            .doOnNext {
                cacheData = it
            }
    }

    operator fun invoke(sourceFrom: FootballDataSourceFrom) = execute(
        mapOf(
            EXTRA_SOURCE_FROM to sourceFrom
        )
    )

    operator fun invoke(sourceFrom: FootballDataSourceFrom, html: String) = execute(
        mapOf(
            EXTRA_SOURCE_FROM to sourceFrom,
            EXTRA_HTML_PAGE to html
        )
    )

    companion object {
        private const val EXTRA_SOURCE_FROM = "extra:source_from"
        private const val EXTRA_HTML_PAGE = "extra:html_page"
    }
}
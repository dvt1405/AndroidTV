package com.kt.apps.media.xemtv.ui.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityTvSearchBinding
import javax.inject.Inject


class TVSearchActivity : BaseActivity<ActivityTvSearchBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: SearchViewModels by lazy {
        ViewModelProvider(this, factory)[SearchViewModels::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.activity_tv_search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initView(savedInstanceState: Bundle?) {
        viewModel.getDefaultSearchList()
        setBackgroundOverlay()
        commitSearchFragment(intent)
    }

    private fun executeSearchFromIntent(searchIntent: Intent) {
        if (Intent.ACTION_SEARCH == searchIntent.action) {
            searchIntent.getStringExtra(SearchManager.QUERY)?.also { query ->
                viewModel.querySearch(query)
            }
        } else if (searchIntent.data?.lastPathSegment == "search") {
            val filter = searchIntent.data?.getQueryParameter("filter")
            searchIntent.data?.getQueryParameter("query")?.takeIf {
                it.trim().isNotBlank()
            }?.let {
                viewModel.querySearch(it, filter)
            } ?: let {
                if (filter == SearchForText.FILTER_ONLY_TV_CHANNEL) {
                    viewModel.querySearch("", filter)
                }
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewModel.searchQueryLiveData.observe(this) {

        }

    }

    private fun executeSearch(query: String, filter: String? = null) {

    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentById(android.R.id.content)
            ?.let {
                super.onBackPressed()
            }
            ?: supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is TVSearchFragment
                }?.let {
                    (it as TVSearchFragment).apply {
                        this.onBackPressed()
                    }
                }
            ?: super.onBackPressed()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        commitSearchFragment(intent)
    }

    override fun onStop() {
        super.onStop()
    }

    private fun commitSearchFragment(extraIntent: Intent?) {

        val filter = extraIntent?.data?.getQueryParameter("filter")
        val query = if (Intent.ACTION_SEARCH == extraIntent?.action) {
            extraIntent.getStringExtra(SearchManager.QUERY)
        } else {
            extraIntent?.data?.getQueryParameter("query")
        }
        val queryHint = extraIntent?.data?.getQueryParameter("query_hint")
        val tvSearchFragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment).takeIf {
            it is TVSearchFragment
        }
        if (tvSearchFragment == null || tvSearchFragment.isDetached
            || tvSearchFragment.isRemoving || tvSearchFragment.isHidden
        ) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_browse_fragment, TVSearchFragment().apply {
                    arguments = bundleOf(
                        TVSearchFragment.EXTRA_QUERY_KEY to query,
                        TVSearchFragment.EXTRA_QUERY_FILTER to filter,
                        TVSearchFragment.EXTRA_QUERY_HINT to queryHint,

                        )
                })
                .commitNow()
        } else {
            (tvSearchFragment as TVSearchFragment).onSearchFromDeeplink(
                query,
                filter,
                queryHint
            )
        }

        extraIntent?.let {
            executeSearchFromIntent(it)
        }
    }
}
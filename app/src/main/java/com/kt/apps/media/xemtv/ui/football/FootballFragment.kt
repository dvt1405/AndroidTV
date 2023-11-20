package com.kt.apps.media.xemtv.ui.football

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.HeaderItem
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.OnItemViewSelectedListener
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.presenter.FootballPresenter
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class FootballFragment : BaseRowSupportFragment() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val footballViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FootballViewModel::class.java]
    }
    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initView(rootView: View) {
        adapter = mRowsAdapter
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is TVChannel) {

            }
        }

        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is FootballMatch) {
                footballViewModel.getLinkStreamFor(item)
            }
        }
    }

    override fun initAction(rootView: View) {
        if (footballViewModel.listFootMatchDataState.value !is DataState.Success) {
            footballViewModel.getAllMatches()
        }
        footballViewModel.listFootMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    mRowsAdapter.clear()
                    progressManager.hide()
                    selectedPosition = -1
                    val calendar = Calendar.getInstance(Locale.TAIWAN)
                    val currentTime = calendar.timeInMillis / 1000
                    it.data.forEach {
                        Logger.d(this, it.getMatchName(), "${currentTime - it.kickOffTimeInSecond}")
                    }
                    val liveMatches = it.data.filter { match ->
                        (currentTime - match.kickOffTimeInSecond) > -20 * 60
                                && (currentTime - match.kickOffTimeInSecond) < 150 * 60
                    }

                    if (liveMatches.isNotEmpty()) {
                        val liveMatchAdapter = ArrayObjectAdapter(FootballPresenter(false))
                        liveMatchAdapter.addAll(0, liveMatches)
                        mRowsAdapter.add(ListRow(HeaderItem("Đang diễn ra"), liveMatchAdapter))
                    }

                    val footballGroupByLeague = it.data.groupBy {
                        it.league
                    }.toSortedMap(Comparator { o1, o2 ->
                        if (o2.lowercase().contains("c1") || o2.lowercase().contains("euro")
                            || o2.lowercase().contains("epl") || o2.lowercase().contains("laliga")
                            || (o2.lowercase().contains("premier") && o2.lowercase().contains("league"))
                            || o2.lowercase().contains("nba")
                            || o2.lowercase().contains("uefa")
                            || o2.lowercase().contains("european")
                            || (o2.lowercase().contains("ngoại") && o2.lowercase().contains("hạng")
                                    && o2.lowercase().contains("anh"))
                        ) {
                            return@Comparator 1
                        }
                        return@Comparator o1.compareTo(o2)
                    })
                    val footballPresenter = FootballPresenter(false)
                    for ((group, channelList) in footballGroupByLeague) {
                        val headerItem = HeaderItem(group)
                        val adapter = ArrayObjectAdapter(footballPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                    }
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }

                is DataState.Loading -> {
                    mRowsAdapter.applyLoading()
                }

                else -> {
                    progressManager.hide()

                }
            }
        }
        footballViewModel.footMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    val data = it.data
                    Logger.d(this, message = "$data")
                    val intent = Intent(requireActivity(), PlaybackActivity::class.java).apply {
                        this.data = null
                        putExtra(PlaybackActivity.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.FOOTBALL as Parcelable)
                        putExtra(PlaybackActivity.EXTRA_FOOTBALL_MATCH, data as Parcelable)
                    }
                    startActivity(intent)
                }

                is DataState.Loading -> {
                }

                is DataState.Error -> {
                    Logger.e(this, exception = it.throwable)
                    showErrorDialog(content = it.throwable.message)
                }

                else -> {
                }
            }
            if (it is DataState.Loading) {
                progressManager.show()
            } else {
                progressManager.hide()
            }
        }
    }

    override fun onDestroyView() {
        mRowsAdapter.clear()
        super.onDestroyView()
    }

    override fun onStop() {
        footballViewModel.clearState()
        super.onStop()
    }

}
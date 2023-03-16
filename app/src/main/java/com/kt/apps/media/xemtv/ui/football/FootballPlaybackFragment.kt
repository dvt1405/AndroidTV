package com.kt.apps.media.xemtv.ui.football

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FootballPlaybackFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val footballViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FootballViewModel::class.java]
    }
    override val numOfRowColumns: Int
        get() = 4
    private var observer: Observer<DataState<FootballMatchWithStreamLink>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        footballViewModel.getAllMatches()
        activity?.intent?.extras?.getParcelable<FootballMatchWithStreamLink>(PlaybackActivity.EXTRA_FOOTBALL_MATCH)
            ?.let {
                playVideo(
                    it.match.getMatchName(),
                    it.match.league,
                    it.linkStreams.map { streamWithReferer ->
                        LinkStream(
                            streamWithReferer.m3u8Link,
                            streamWithReferer.referer,
                            streamWithReferer.m3u8Link
                        )
                    },
                    isLive = it.match.isLiveMatch
                )
            }
        observer = Observer { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    progressBarManager.hide()
                    val data = dataState.data
                    val linkStreams = data.linkStreams.map { streamWithReferer ->
                        LinkStream(
                            streamWithReferer.m3u8Link,
                            streamWithReferer.referer,
                            streamWithReferer.m3u8Link
                        )
                    }
                    playVideo(
                        data.match.getMatchName(),
                        data.match.league,
                        linkStreams,
                        isLive = data.match.isLiveMatch
                    )
                }

                is DataState.Loading -> {
                    progressBarManager.show()
                }

                is DataState.Error -> {
                    progressBarManager.hide()
                    Logger.e(this, exception = dataState.throwable)
                    showErrorDialog(
                        content = dataState.throwable.message,
                        onSuccessListener = {
                            startActivity(Intent().apply {
                                data = Uri.parse("xemtv://bongda/dashboard")
                            })
                        },
                    )
                }

                else -> {

                }
            }
        }
        footballViewModel.footMatchDataState.observe(viewLifecycleOwner, observer!!)

        footballViewModel.listFootMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    setupRowAdapter(it.data.filter {
                        it.isLiveMatch
                    }.ifEmpty {
                        it.data.sortedBy {
                            it.kickOffTimeInSecond
                        }
                    }, TVChannelPresenterSelector(requireActivity()))
                    onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
                        footballViewModel.getLinkStreamFor(item as FootballMatch)
                    }
                }

                else -> {

                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        Logger.d(this, message = "onStop")
        footballViewModel.clearState()
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }
}
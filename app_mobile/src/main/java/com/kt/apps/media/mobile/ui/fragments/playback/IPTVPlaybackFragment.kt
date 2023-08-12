package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.util.Util
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.ui.view.RowItemChannelAdapter
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.fillParent
import com.kt.apps.media.mobile.utils.matchParentWidth
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.safeLet
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.IPTVPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadIPTVJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class IPTVPlaybackFragment : ChannelPlaybackFragment() {

    private val durationTV: TextView? by lazy {
        exoPlayer?.findViewById(R.id.tv_live_time)
    }
    private val _playbackViewModel by lazy {
        IPTVPlaybackInteractor(
            ViewModelProvider(requireActivity(), factory),
            viewLifecycleOwner.lifecycleScope
        )
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

    private val itemAdapter by lazy {
        RowItemChannelAdapter()
    }

    private val stringBuilder = StringBuilder()
    private val formatter = Formatter(stringBuilder, Locale.getDefault())
    private var isSeeking = AtomicBoolean(false)
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        channelListRecyclerView?.apply {
            adapter = itemAdapter
            addItemDecoration(channelItemDecoration)
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
        }
        categoryLabel?.text = (arguments?.get(EXTRA_EXTENSION_GROUP) as? String)
        progressBar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    isSeeking.set(true)
                    exoPlayer?.controllerShowTimeoutMs = 0
                    exoPlayer?.player?.run {
                        durationTV?.text = "${Util.getStringForTime(stringBuilder, formatter, progress.toLong())}/${Util.getStringForTime(stringBuilder, formatter, contentDuration)}"
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isSeeking.set(true)
                exoPlayer?.controllerShowTimeoutMs = 0
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                exoPlayer?.player?.seekTo(p0?.progress?.toLong() ?: 0)
                isSeeking.set(false)
                exoPlayer?.controllerShowTimeoutMs = 1000
            }
        })
    }

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        val extensionsChannel = arguments?.get(EXTRA_TV_CHANNEL) as? ExtensionsChannel
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                merge(
                    extensionsChannel?.let { flowOf(it) } ?: emptyFlow(),
                    itemAdapter.childClicks().mapNotNull {
                        (it as? ChannelElement.ExtensionChannelElement)?.model
                    }
                ).collectLatest {
                    _playbackViewModel.loadIPTVJob(it)
                }
            }

            launch {
                combine(
                    (((arguments?.get(EXTRA_EXTENSION_ID) as? String)?.let { flowOf(it) }) ?: flowOf("")),
                    (((arguments?.get(EXTRA_EXTENSION_GROUP) as? String)?.let { flowOf(it) }) ?: flowOf(""))
                ) { id, group -> Pair(id, group)}
                    .collectLatest {
                        _playbackViewModel.loadChannelConfig(it.first, it.second)
                    }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                _playbackViewModel.relatedItems
                    .collectLatest {
                       itemAdapter.onRefresh(it)
                    }
            }

            launch {
                while(true) {
                    exoPlayer?.player?.run {
                        if (playbackState == ExoPlayer.STATE_READY) {
                            if (!isSeeking.get()) {
                                durationTV?.text = "${Util.getStringForTime(stringBuilder, formatter, contentPosition)}/${Util.getStringForTime(stringBuilder, formatter, contentDuration)}"
                                progressBar?.progress= contentPosition.toInt()
                                progressBar?.secondaryProgress = bufferedPosition.toInt()
                            }
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == ExoPlayer.STATE_READY) {
            if(exoPlayer?.player?.isCurrentMediaItemLive == true) {
                progressBar?.visibility = View.GONE
                durationTV?.visibility = View.GONE
                liveLabel?.visibility = View.VISIBLE
            } else {
                progressBar?.visibility = View.VISIBLE
                durationTV?.visibility = View.VISIBLE
                liveLabel?.visibility = View.GONE
                progressBar?.apply {
                    isEnabled = true
                    max = exoPlayer?.player?.duration?.toInt() ?: 0
                    progress = 0
                }
            }
        }
    }
    companion object {
        const val screenName = "IPTVPlaybackFragment"
        private const val EXTRA_EXTENSION_ID = "extra:extension_id"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        private const val EXTRA_EXTENSION_GROUP = "extra:extension_group"
        fun newInstance(
            tvChannel: ExtensionsChannel,
            extension: String,
            groupTitle: String
        ) = IPTVPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_EXTENSION_ID to extension,
                EXTRA_EXTENSION_GROUP to groupTitle
            )
        }
    }
}
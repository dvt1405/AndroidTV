package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.PlaybackException
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.logging.logPlaybackRetryGetStreamLink
import com.kt.apps.core.logging.logPlaybackRetryPlayVideo
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.RowItemChannelAdapter
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.RadioPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.TVPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadLinkStreamChannel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class TVPlaybackFragment: ChannelPlaybackFragment() {
    private val _playbackInteractor by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }

    override val interactor: BasePlaybackInteractor
        get() = _playbackInteractor

    private val itemAdapter by lazy {
        RowItemChannelAdapter()
    }
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        progressBar?.visibility = View.GONE
        categoryLabel?.text = getString(R.string.tv_page_title)
        channelListRecyclerView?.apply {
            adapter = itemAdapter
            addItemDecoration(channelItemDecoration)
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }
    }
    private val mapRetryTimes by lazy {
        mutableMapOf<String, Int>()
    }

    override fun onPlayerError(error: PlaybackException) {
        fun notifyError() {
            lifecycleScope.launch {
                interactor.playbackError(PlaybackThrowable(error.errorCode, error))
            }
        }
        val retriedTimes = try {
            mapRetryTimes[_playbackInteractor.tvChannelViewModel.lastWatchedChannel?.channel?.channelId] ?: 0
        } catch (e: Exception) {
            0
        }

        when {
            retriedTimes > BasePlaybackFragment.MAX_RETRY_TIME || (_playbackInteractor.tvChannelViewModel.lastWatchedChannel?.linkReadyToStream?.size ?: 0) == 0 -> {
                notifyError()
            }

            (_playbackInteractor.tvChannelViewModel.lastWatchedChannel?.linkReadyToStream?.size ?: 0) > 1 -> {
                val newLinks = _playbackInteractor.tvChannelViewModel.lastWatchedChannel!!.linkReadyToStream
                val newStreamList = if (newLinks.isNotEmpty()) {
                    newLinks.subList(
                        1,
                        newLinks.size
                    )
                } else emptyList()

                val newChannelWithLink = TVChannelLinkStream(
                    _playbackInteractor.tvChannelViewModel.lastWatchedChannel!!.channel,
                    newStreamList
                )
                _playbackInteractor.tvChannelViewModel.markLastWatchedChannel(newChannelWithLink)
                val newLinkData = if (executingIndex + 1 <= newLinks.size) {
                    newLinks.subList(
                        executingIndex + 1, newLinks.size
                    )
                } else emptyList()

                val newExecutingData = StreamLinkData.TVStreamLinkData(newChannelWithLink)


                if (newLinkData.isNotEmpty()) {
                    val newStreamLinkData = StreamLinkData.Custom(
                        title = newExecutingData.title,
                        linkStream = newExecutingData.linkStream,
                        isHls = newExecutingData.isHls,
                        streamId = newExecutingData.streamId,
                        itemMetaData = newExecutingData.itemMetaData
                    )
                    lifecycleScope.launch {
                        playVideo(newStreamLinkData)
                        _playbackInteractor.tvChannelViewModel.actionLogger.logPlaybackRetryPlayVideo(
                            error,
                            _playbackInteractor.tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "Unknown",
                            "link" to newStreamList.first()
                        )
                        mapRetryTimes[_playbackInteractor.tvChannelViewModel.lastWatchedChannel?.channel!!.channelId] = retriedTimes + 1
                    }
                    return
                }
            }

            else -> {
                _playbackInteractor.tvChannelViewModel.retryGetLastWatchedChannel()
                _playbackInteractor.tvChannelViewModel.actionLogger.logPlaybackRetryGetStreamLink(
                    error,
                    _playbackInteractor.tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "Unknown"
                )
                mapRetryTimes[_playbackInteractor.tvChannelViewModel.lastWatchedChannel?.channel!!.channelId] = retriedTimes + 1
            }
        }
    }

    @OptIn(FlowPreview::class)
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)


        val itemToPlay = if(_playbackInteractor.currentPlayingVideo.value == null) {
            arguments?.get(EXTRA_TV_CHANNEL) as? TVChannel
        } else null

        lifecycleScope.launch {
            val loadItemFlow: Flow<ChannelElement.TVChannelElement> = merge(
                itemAdapter.childClicks().mapNotNull { it as? ChannelElement.TVChannelElement },
                itemToPlay?.let { flowOf(it) }?.map {  ChannelElement.TVChannelElement(it)} ?: emptyFlow()
            ).stateIn(lifecycleScope)

            repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED)  {
                lifecycleScope.launch(CoroutineExceptionHandler(coroutineError())) {
                    loadItemFlow.collectLatest {
                        title.emit(it.model.tvChannelName)
                        _playbackInteractor.loadProgramForChannel(it)
                        _playbackInteractor.loadLinkStreamChannel(it)
                    }
                }

                lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->
                    subTitle?.gone()
                })  {
                    _playbackInteractor.currentProgrammeForChannel
                        .mapNotNull { it }
                        .collectLatest { infor ->
                            infor.title.takeIf { t -> t.isNotBlank() }
                                ?.run {
                                    subTitle?.visible()
                                    subTitle?.text = this
                                }
                                ?: kotlin.run {
                                    subTitle?.gone()
                                }
                        }
                }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                _playbackInteractor.channelElementList.collectLatest {
                    itemAdapter.onRefresh(it)
                }
            }
        }
    }

    override suspend fun preparePlayView(data: PrepareStreamLinkData) {
        super.preparePlayView(data)
        liveLabel?.visibility = View.GONE
    }
    override suspend fun playVideo(data: StreamLinkData) {
        super.playVideo(data)
        liveLabel?.visibility = View.VISIBLE
    }
    companion object {
        const val screenName: String = "TVPlaybackFragment"
        const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun newInstance(
            tvChannel: TVChannel
        ) = TVPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
            )
        }
    }
}

class RadioPlaybackFragment: ChannelPlaybackFragment() {
    private val _playbackInteractor by lazy {
        RadioPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val interactor: BasePlaybackInteractor
        get() = _playbackInteractor

    private val itemAdapter by lazy {
        RowItemChannelAdapter()
    }
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        progressBar?.visibility = View.GONE
        categoryLabel?.text = getString(R.string.radio_page_title)
        channelListRecyclerView?.apply {
            adapter = itemAdapter
            addItemDecoration(channelItemDecoration)
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }

        binding.exoPlayer.useArtwork = true
        binding.exoPlayer.defaultArtwork = resources.getDrawable(com.kt.apps.core.R.drawable.bg_radio_playing   )
    }
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        val itemToPlay = if(_playbackInteractor.currentPlayingVideo.value == null) {
            arguments?.get(TVPlaybackFragment.EXTRA_TV_CHANNEL) as? TVChannel
        } else null

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            val loadItemFlow = merge(
                itemToPlay?.let { flowOf(ChannelElement.TVChannelElement(it)) } ?: emptyFlow(),
                itemAdapter.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
            ).stateIn(lifecycleScope)

            lifecycleScope.launch(CoroutineExceptionHandler(coroutineError())) {
                loadItemFlow.collectLatest {
                    title.emit(it.model.tvChannelName)
                    _playbackInteractor.loadProgramForChannel(it)
                    _playbackInteractor.loadLinkStreamChannel(it)
                }
            }

            lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->
                subTitle?.gone()
            }) {
                _playbackInteractor.currentProgrammeForChannel
                    .mapNotNull { it }
                    .collectLatest { infor ->
                        infor.title.takeIf { t -> t.isNotBlank() }
                            ?.run {
                                subTitle?.visible()
                                subTitle?.text = this
                            }
                            ?: kotlin.run {
                                subTitle?.gone()
                            }
                    }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                _playbackInteractor.radioChannelList.collectLatest {
                    itemAdapter.onRefresh(it)
                }
            }
        }
    }

    override suspend fun preparePlayView(data: PrepareStreamLinkData) {
        super.preparePlayView(data)
        liveLabel?.visibility = View.GONE
        (data as? PrepareStreamLinkData.Radio)?.run {
            loadArtwork(this.data)
        }
    }

    override suspend fun playVideo(data: StreamLinkData) {
        super.playVideo(data)
        liveLabel?.visibility = View.VISIBLE
        (data as? StreamLinkData.TVStreamLinkData)?.run {
            loadArtwork(this.data.channel)
        }
    }
    private fun loadArtwork(data: TVChannel) {
        context?.run {
            binding.exoPlayer.useArtwork = true
            binding.exoPlayer.defaultArtwork = data.loadImgDrawable(this)
        }
    }

    companion object {
        const val screenName: String = "TVPlaybackFragment"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun newInstance(
            tvChannel: TVChannel
        ) = RadioPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
            )
        }
    }
}
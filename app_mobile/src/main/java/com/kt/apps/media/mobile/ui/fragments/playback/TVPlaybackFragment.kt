package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.inVisible
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class
TVPlaybackFragment: ChannelPlaybackFragment() {
    private val _playbackInteractor by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }

    override val playbackViewModel: BasePlaybackInteractor
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
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
        }
    }

    @OptIn(FlowPreview::class)
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)

        val itemToPlay = arguments?.get(EXTRA_TV_CHANNEL) as? TVChannel


        lifecycleScope.launch {
            val loadItemFlow: Flow<ChannelElement.TVChannelElement> = merge(
                itemAdapter.childClicks().mapNotNull { it as? ChannelElement.TVChannelElement },
                itemToPlay?.let { flowOf(it) }?.map {  ChannelElement.TVChannelElement(it)} ?: emptyFlow()
            ).stateIn(lifecycleScope)

            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch(coroutineError()) {
                    loadItemFlow.collectLatest {
                        title.emit(it.model.tvChannelName)
                        _playbackInteractor.loadProgramForChannel(it)
                        _playbackInteractor.loadLinkStreamChannel(it)
                    }
                }

                launch(exceptionHandler { _, _ ->
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
    override val playbackViewModel: BasePlaybackInteractor
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
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
        }

        binding.exoPlayer.useArtwork = true
        binding.exoPlayer.defaultArtwork = resources.getDrawable(com.kt.apps.core.R.drawable.bg_radio_playing   )
    }
    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        val itemToPlay = arguments?.get(TVPlaybackFragment.EXTRA_TV_CHANNEL) as? TVChannel

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            val loadItemFlow = merge(
                itemToPlay?.let { flowOf(ChannelElement.TVChannelElement(it)) } ?: emptyFlow(),
                itemAdapter.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
            ).stateIn(lifecycleScope)

            launch(coroutineError()) {
                loadItemFlow.collectLatest {
                    title.emit(it.model.tvChannelName)
                    _playbackInteractor.loadProgramForChannel(it)
                    _playbackInteractor.loadLinkStreamChannel(it)
                }
            }

            launch(NonCancellable + CoroutineExceptionHandler { _, throwable ->
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
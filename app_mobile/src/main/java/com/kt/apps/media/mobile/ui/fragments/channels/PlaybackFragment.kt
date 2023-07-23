package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.*
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView.ControllerVisibilityListener
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.dialog.JobQueue
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelFragment
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.clicks
import com.kt.apps.media.mobile.utils.ktFadeIn
import com.kt.apps.media.mobile.utils.ktFadeOut
import com.kt.apps.media.mobile.viewmodels.PlaybackControlViewModel
import com.kt.apps.media.mobile.viewmodels.StreamLinkData
import com.pnikosis.materialishprogress.ProgressWheel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface IPlaybackAction {
    fun onLoadedSuccess(videoSize: VideoSize)
    fun onOpenFullScreen()

    fun onPauseAction(userAction: Boolean)
    fun onPlayAction(userAction: Boolean)

    fun onExitMinimal()
}

class PlaybackFragment : BaseFragment<FragmentPlaybackBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_playback
    override val screenName: String
        get() = "Fragment Playback"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile

    var callback: IPlaybackAction? = null
    private var _cachePlayingState: Boolean = false
    //Views
    private val fullScreenButton: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
    }

    private val playPauseButton: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_play_pause)
    }


    private val progressWheel: View by lazy {
        binding.exoPlayer.findViewById(R.id.progress_bar_container)
    }

    private val backButton: MaterialButton by lazy {
        binding.exoPlayer.findViewById(R.id.back_button)
    }

    private val channelFragmentContainer: FragmentContainerView by lazy {
        binding.exoPlayer.findViewById(R.id.player_overlay_container)
    }

    private var shouldShowChannelList: Boolean = false

    private val playbackViewModel by lazy {
        PlaybackControlViewModel(ViewModelProvider(requireActivity(), factory))
    }

    private val playerListener: Player.Listener = object: Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            Log.d(TAG, "onStateChange onIsPlayingChanged: $isPlaying")
            if (isPlaying) {
                progressWheel.fadeOut {
                    playPauseButton.fadeIn {  }
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            Log.d(TAG, "onStateChange onIsLoadingChanged: $isLoading")
            if (isLoading) {
                playPauseButton.fadeOut {
                    progressWheel.fadeIn {  }
                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding) {
            exoPlayer.player = exoPlayerManager.exoPlayer
            exoPlayer.showController()
            exoPlayer.setShowNextButton(false)
            exoPlayer.setShowPreviousButton(false)
            exoPlayer.setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
                if (visibility != View.VISIBLE)
                    channelFragmentContainer.visibility = visibility
                else
                    if (shouldShowChannelList)
                        showHideChannelList(isShow = true)
            })

            playPauseButton.visibility = View.INVISIBLE
            progressWheel.visibility = View.INVISIBLE
        }


        fullScreenButton.visibility = View.VISIBLE
        fullScreenButton.setOnClickListener {
            callback?.onOpenFullScreen()
        }

        playPauseButton.setOnClickListener {
            exoPlayerManager.exoPlayer?.run {
                shouldShowChannelList = if (isPlaying) {
                    pause()
                    showHideChannelList(isShow = true)
                    true
                } else {
                    play()
                    showHideChannelList(isShow = false)
                    false
                }
            } ?: kotlin.run {
//                tvChannelViewModel?.tvWithLinkStreamLiveData?.value?.run {
//                    when(this) {
//                        is DataState.Success -> {
//                            playVideo(data)
//                        }
//                        else -> {}
//                    }
//                }
            }
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            exoPlayerManager.exoPlayer?.pause()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            exoPlayerManager.exoPlayer?.play()
            return true
        }
        return false
    }

    @OptIn(FlowPreview::class)
    override fun initAction(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            playbackViewModel.streamData.collectLatest {
                shouldShowChannelList = false
                playVideo(it)
            }
        }

        playbackViewModel.playbackState
            .onEach {
                if (it == PlaybackState.Minimal) {
                    changeMinimalLayout()
                } else {
                    changeFullScreenLayout()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playbackViewModel.progressLinkState
            .filter { it is PlaybackViewModel.State.LOADING }
            .onEach { preparePlayView() }
            .launchIn(lifecycleScope)

        merge(backButton.clicks(), binding.exitButton?.clicks() ?: emptyFlow())
            .debounce(250)
            .onEach {
                callback?.onExitMinimal()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                launch {
//                    playbackViewModel?.state?.map {
//                        it == PlaybackViewModel.State.LOADING
//                    }?.collectLatest {
//                        toggleProgressing(it)
//                    }
//                }
//
//                launch {
//                    isProcessing
//                        .collectLatest {
//                            if (it) {
//                                animationQueue.submit(kotlin.coroutines.coroutineContext) {
//                                    binding.exoPlayer.showController()
//                                    progressWheel.visibility = View.VISIBLE
//                                    awaitAll(async {
//                                        progressWheel.ktFadeIn()
//                                        progressHelper.spin()
//                                    })
//                                }
//                            } else {
//                                animationQueue.submit(kotlin.coroutines.coroutineContext) {
//                                    awaitAll(async {
//                                        progressWheel.ktFadeOut()
//                                        progressHelper.stopSpinning()
//                                    })
//                                }
//                            }
//                        }
//                }
//
//                launch {
//                    playbackViewModel?.displayState?.collectLatest {state ->
//                        if (state != PlaybackState.Fullscreen) {
//                            showHideChannelList(false)
//                        }
//                    }
//                }
//
//                launch {
//                    playbackViewModel?.displayState?.mapLatest {
//                        when(it) {
//                            PlaybackState.Fullscreen -> com.google.android.exoplayer2.R.drawable.exo_ic_fullscreen_exit
//                            else -> com.google.android.exoplayer2.R.drawable.exo_controls_fullscreen_enter
//                        }
//                    }?.collectLatest {
//                        fullScreenButton.setImageResource(it)
//                    }
//                }
//            }
//        }
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
        _cachePlayingState = exoPlayerManager.exoPlayer?.isPlaying ?: false
        exoPlayerManager.pause()
        shouldShowChannelList = false
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        shouldShowChannelList = false
        _cachePlayingState = if (_cachePlayingState) {
            exoPlayerManager.exoPlayer?.play()
            false
        } else false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        childFragmentManager.beginTransaction()
            .replace(R.id.player_overlay_container, LightweightChannelFragment.newInstance())
            .commit()

        channelFragmentContainer.visibility = View.INVISIBLE
    }

    private fun changeFullScreenLayout() {
        binding.minimalLayout?.fadeOut {
            binding.exoPlayer.useController = true
            binding.exoPlayer.showController()
        }

        binding.motionLayout?.transitionToState(R.id.start)
    }

    private fun changeMinimalLayout() {
        binding.exoPlayer.apply {
            useController = false
            hideController()
        }
        binding.motionLayout?.transitionToState(R.id.end)
    }

    private fun preparePlayView() {
        exoPlayerManager.exoPlayer?.stop()
        playPauseButton.visibility = View.INVISIBLE
        progressWheel.fadeIn {  }
    }

    private fun playVideo(data: StreamLinkData) {
        exoPlayerManager.playVideo(data.linkStream.map {
            LinkStream(it, data.webDetailPage, data.webDetailPage)
        }, data.isHls, data.itemMetaData , playerListener)
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        MainScope().launch {
            backButton.text = data.title
            binding.minimalTitleTv?.text = data.title
        }
    }

    private fun stopCurrentVideo() {
        exoPlayerManager.exoPlayer?.stop()
    }

    private val animationQueue: JobQueue by lazy {
        JobQueue()
    }

    private fun showHideChannelList(isShow: Boolean) {
//        val displayState  = playbackViewModel?.displayState?.value ?: PlaybackState.Invisible
//        if (isShow && displayState == PlaybackState.Fullscreen) {
//            channelFragmentContainer.fadeIn {  }
//            return
//        }
//        if (!isShow && displayState == PlaybackState.Fullscreen) {
//            channelFragmentContainer.fadeOut {  }
//            return
//        }
//        channelFragmentContainer.visibility = View.INVISIBLE
    }

}
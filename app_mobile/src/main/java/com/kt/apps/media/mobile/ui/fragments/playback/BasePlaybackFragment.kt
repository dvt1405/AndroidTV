package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import androidx.lifecycle.*
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.utils.clicks
import com.kt.apps.media.mobile.viewmodels.BasePlaybackControlViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

interface IPlaybackAction {
    fun onLoadedSuccess(videoSize: VideoSize)
    fun onOpenFullScreen()

    fun onPauseAction(userAction: Boolean)
    fun onPlayAction(userAction: Boolean)

    fun onExitMinimal()
}

abstract class BasePlaybackFragment : BaseFragment<FragmentPlaybackBinding>() {

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

    private val minimalProgress by lazy {
        binding.minimalLoading
    }

    private val minimalPlayPause by lazy {
        binding.minimalPlayButton
    }

    private val backButton: MaterialButton by lazy {
        binding.exoPlayer.findViewById(R.id.back_button)
    }

    private val titleLabel: MaterialTextView by lazy {
        binding.exoPlayer.findViewById(R.id.title)
    }

    private val title = MutableStateFlow("")
    private val isProgressing = MutableStateFlow(false)

    private var shouldShowChannelList: Boolean = false

    protected abstract val playbackViewModel: BasePlaybackControlViewModel

    private val playerListener: Player.Listener = object: Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.d(TAG, "onPlayerError: $error")
            MainScope().launch {
                playbackViewModel.playbackError(PlaybackThrowable(error.errorCode, error))
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            Log.d(TAG, "onVideoSizeChanged: $videoSize")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            Log.d(TAG, "onStateChange onIsPlayingChanged: $isPlaying")
            if (isPlaying) {
                MainScope().launch {
                    isProgressing.emit(false)
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            Log.d(TAG, "onStateChange onIsLoadingChanged: $isLoading")
            if (isLoading) {
                MainScope().launch {
                    isProgressing.emit(true)
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

            playPauseButton.visibility = View.INVISIBLE
            progressWheel.visibility = View.INVISIBLE
        }


        fullScreenButton.visibility = View.VISIBLE
        fullScreenButton.setOnClickListener {
            callback?.onOpenFullScreen()
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

        playbackViewModel.playbackState
            .onEach {
                if (it == PlaybackState.Minimal) {
                    changeMinimalLayout()
                } else {
                    changeFullScreenLayout()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playbackViewModel.loadEvents
            .onEach { preparePlayView(it.data) }
            .launchIn(lifecycleScope)

        playbackViewModel.streamLinkEvents
            .onEach { playVideo(it.data) }
            .launchIn(lifecycleScope)

        playbackViewModel.errorEvents
            .mapNotNull { it }
            .onEach { onError(it.error) }
            .launchIn(lifecycleScope)

        merge(backButton.clicks(), binding.exitButton?.clicks() ?: emptyFlow())
            .debounce(250)
            .onEach {
                exoPlayerManager.exoPlayer?.stop()
                callback?.onExitMinimal()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        title.onEach {
            titleLabel.text = it
            binding.minimalTitleTv?.text = it
        }
            .launchIn(viewLifecycleOwner.lifecycleScope + Dispatchers.Main)

        isProgressing.onEach {isProgressing ->
            if (isProgressing) {
                playPauseButton.fadeOut {  }
                progressWheel.fadeIn {  }
                minimalProgress?.visibility = View.VISIBLE
                minimalPlayPause?.visibility = View.GONE
            } else {
                playPauseButton.fadeIn {  }
                progressWheel.fadeOut {  }
                minimalProgress?.visibility = View.GONE
                minimalPlayPause?.visibility = View.VISIBLE
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope + Dispatchers.Main)

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

//
//        childFragmentManager.beginTransaction()
//            .replace(R.id.player_overlay_container, LightweightChannelFragment.newInstance())
//            .commit()
//
//        channelFragmentContainer.visibility = View.INVISIBLE
    }

    private fun onError(throwable: Throwable?) {
        val errorCode = (throwable as? PlaybackThrowable)?.code ?: -1
        showErrorDialog(
            titleText = "Lỗi phát video",
            content = "Xin lỗi, mở nội dung không thành công. Vui lòng thử lại sau.\nMã lỗi: $errorCode",
            cancellable = false,
            onDismissListener = {
                backButton.performClick()
            })
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

    private fun preparePlayView(data: PrepareStreamLinkData) {
        exoPlayerManager.exoPlayer?.stop()
        exoPlayerManager.exoPlayer?.removeListener(playerListener)
        playPauseButton.visibility = View.INVISIBLE
        progressWheel.fadeIn {  }
        title.value = data.title
    }

    private fun playVideo(data: StreamLinkData) {
        exoPlayerManager.playVideo(data.linkStream.map {
            LinkStream(it, data.webDetailPage, data.webDetailPage)
        }, data.isHls, data.itemMetaData , playerListener)
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        title.value = data.title
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
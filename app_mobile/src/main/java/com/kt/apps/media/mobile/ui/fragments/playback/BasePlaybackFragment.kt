package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageButton
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.*
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.ads.interactivemedia.v3.internal.it
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.complex.LandscapeLayoutHandler
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.complex.TransitionCallback
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
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

enum class DisplayState {
    Fullscreen, Minimal
}

abstract class BasePlaybackFragment : BaseFragment<FragmentPlaybackBinding>(), IDispatchTouchListener {

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

    protected val channelListRecyclerView: ChannelListView? by lazy {
        binding.channelList
    }

    private val title = MutableStateFlow("")
    private val isProgressing = MutableStateFlow<Boolean>(false)
    protected val isShowChannelList = MutableStateFlow(false)
    private val displayState = MutableStateFlow(DisplayState.Fullscreen)

    protected abstract val playbackViewModel: BasePlaybackInteractor

    private val gestureDetector by lazy {
        object: OnSwipeTouchListener(requireContext()) {
            override fun onSwipeTop() {
                lifecycleScope.launch {
                    isShowChannelList.emit(true)
                }
            }

            override fun onSwipeBottom() {
                lifecycleScope.launch {
                    isShowChannelList.emit(false)
                }
            }

            override fun onFling() {
                binding.exoPlayer.controllerShowTimeoutMs = 0
            }
        }
    }

    private var playerListener: Player.Listener = object: Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.d(TAG, "onPlayerError: $error")
            lifecycleScope.launch {
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
                lifecycleScope.launch {
                    isProgressing.emit(false)
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            Log.d(TAG, "onStateChange onIsLoadingChanged: $isLoading")
            if (isLoading) {
                lifecycleScope.launch {
                    isProgressing.emit(true)
                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView:")
        with(binding) {
            exoPlayer.player = exoPlayerManager.exoPlayer
            exoPlayer.showController()
            exoPlayer.setShowNextButton(false)
            exoPlayer.setShowPreviousButton(false)

            playPauseButton.visibility = View.INVISIBLE
            progressWheel.visibility = View.INVISIBLE
        }

        binding.exoPlayer.setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener {
            when(it) {
                View.VISIBLE -> binding.channelList?.visibility = it
                View.GONE, View.INVISIBLE -> binding.channelList?.visibility = View.INVISIBLE
            }
        })


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
        Log.d(TAG, "initAction:")
        lifecycleScope.launchWhenCreated {
            playbackViewModel.state
                .collectLatest { state ->
                    Log.d(TAG, "initAction: state $state ${this@BasePlaybackFragment}")
                    when(state) {
                        is PlaybackViewModel.State.LOADING -> preparePlayView(state.data)
                        is PlaybackViewModel.State.PLAYING -> playVideo(state.data)
                        is PlaybackViewModel.State.ERROR -> onError(state.error)
                        else -> {}
                    }
                }
        }

        merge(backButton.clicks(), binding.exitButton?.clicks() ?: emptyFlow())
            .debounce(250)
            .onEach {
                exoPlayerManager.exoPlayer?.stop()
                callback?.onExitMinimal()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launchWhenCreated {
            title.collect {
                Log.d(TAG, "initAction: state title $it ${this@BasePlaybackFragment}")
                titleLabel.text = it
                binding.minimalTitleTv?.text = it
            }
        }

        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf(
            {
                displayState.collectLatest {
                    when (it) {
                        DisplayState.Fullscreen -> changeFullScreenLayout()
                        DisplayState.Minimal -> changeMinimalLayout()
                    }
                }
            },
            {
                playbackViewModel.playbackState.map {
                    when (it) {
                        PlaybackState.Fullscreen -> DisplayState.Fullscreen
                        PlaybackState.Minimal -> DisplayState.Minimal
                        else -> DisplayState.Fullscreen
                    }
                }.collectLatest { displayState.emit(it) }
            },
            {
                isShowChannelList.collectLatest {
                    if (displayState.value != DisplayState.Fullscreen) return@collectLatest
                    if (it) showChannelList() else changeFullScreenLayout()
                }
            },{
                isProgressing.collectLatest {
                    Log.d(TAG, "initAction: isProgressing $it")
                    toggleProgressingUI(it)
                }
            }
        ))
    }

    override fun onDispatchTouchEvent(view: View?, mv: MotionEvent) {
        leakView?.run {
            gestureDetector.onTouch(this, mv)
        }
    }
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
        _cachePlayingState = exoPlayerManager.exoPlayer?.isPlaying ?: false
        exoPlayerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        exoPlayerManager.detach(playerListener)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        _cachePlayingState = if (_cachePlayingState) {
            exoPlayerManager.exoPlayer?.play()
            false
        } else false
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

    private fun toggleProgressingUI(isProgressing: Boolean) {
        if (isProgressing) {
            playPauseButton.visibility = View.GONE
            progressWheel.visibility = View.VISIBLE
            minimalProgress?.visibility = View.VISIBLE
            minimalPlayPause?.visibility = View.GONE
        } else {
            playPauseButton.visibility = View.VISIBLE
            progressWheel.visibility = View.GONE
            minimalProgress?.visibility = View.GONE
            minimalPlayPause?.visibility = View.VISIBLE
        }
    }
    protected fun changeFullScreenLayout() {
        binding.exoPlayer.useController = true
        binding.exoPlayer.showController()
        binding.exoPlayer.controllerShowTimeoutMs = 1000
        binding.exoPlayer.controllerHideOnTouch = true

        safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
            mainLayout, exoplayer,  minimal, list ->
            performTransition(mainLayout, ConstraintSet().apply {
                clone(this)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                arrayListOf(ConstraintSet.END, ConstraintSet.BOTTOM, ConstraintSet.START, ConstraintSet.TOP).forEach {
                    connect(exoplayer.id, it, ConstraintSet.PARENT_ID, it)
                }
                setVisibility(minimal.id, View.GONE)
                matchParentWidth(list.id)
                connect(list.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                constrainHeight(list.id, ConstraintSet.WRAP_CONTENT)
                setMargin(list.id, ConstraintSet.TOP, (-88).dpToPx())
            })
            isShowChannelList.value = false
        }
    }

    abstract fun provideMinimalLayout(): ConstraintSet?
    private fun changeMinimalLayout() {
        binding.exoPlayer.apply {
            useController = false
            hideController()
        }
        safeLet(binding.motionLayout, provideMinimalLayout()) {
                mainLayout, constraintSet ->
            performTransition(mainLayout, constraintSet)
            isShowChannelList.value = false
        }
    }

    protected fun showChannelList() {
        binding.exoPlayer.showController()
        binding.exoPlayer.controllerShowTimeoutMs = -1
        binding.exoPlayer.controllerHideOnTouch = false
        safeLet(binding.motionLayout, binding.exoPlayer, binding.channelList) {
                mainLayout, exoplayer, list ->
            performTransition(mainLayout, ConstraintSet().apply {
                clone(this)
                clear(exoplayer.id)
                listOf(ConstraintSet.START, ConstraintSet.TOP, ConstraintSet.END).forEach {
                    alignParent(exoplayer.id, it, 20.dpToPx())
                }
                connect(exoplayer.id, ConstraintSet.BOTTOM, list.id, ConstraintSet.TOP, (-88).dpToPx())
                clear(list.id)
                constrainHeight(list.id, ConstraintSet.WRAP_CONTENT)
                matchParentWidth(list.id)
                alignParent(list.id, ConstraintSet.BOTTOM, (-20).dpToPx())
            })
        }
    }
    protected open suspend fun preparePlayView(data: PrepareStreamLinkData) {
        exoPlayerManager.exoPlayer?.stop()
        isProgressing.emit(true)
        title.emit(data.title)
    }

    protected open suspend fun playVideo(data: StreamLinkData) {
        exoPlayerManager.playVideo(data.linkStream.map {
            LinkStream(it, data.webDetailPage, data.webDetailPage)
        }, data.isHls, data.itemMetaData , playerListener)
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        title.emit(data.title)
    }

    private fun performTransition(layout: ConstraintLayout, set: ConstraintSet, onStart: (() -> Unit)? = null) {
        TransitionManager.beginDelayedTransition(layout, AutoTransition().apply {
            interpolator = AccelerateInterpolator()
            duration = 500
            addListener(object: TransitionCallback() {
                override fun onTransitionStart(transition: Transition) {
                    super.onTransitionStart(transition)
                    onStart?.invoke()
                }
            })
        })
        set.applyTo(layout)
    }
}
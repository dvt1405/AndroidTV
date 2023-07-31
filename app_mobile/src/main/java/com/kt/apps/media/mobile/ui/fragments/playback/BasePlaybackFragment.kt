package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.ExoPlayer
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
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.complex.TransitionCallback
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

interface IPlaybackControl {
    var callback: IPlaybackAction?
}

abstract class BasePlaybackFragment<T : ViewDataBinding> : BaseFragment<T>(), IDispatchTouchListener, IPlaybackControl {

    override val screenName: String
        get() = "Fragment Playback"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile


    private var _cachePlayingState: Boolean = false

    override var callback: IPlaybackAction? = null
    //Views
    private val fullScreenButton: ImageButton? by lazy {
        exoPlayer?.findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
    }

    private val playPauseButton: ImageButton? by lazy {
        exoPlayer?.findViewById(com.google.android.exoplayer2.ui.R.id.exo_play_pause)
    }

    private val progressWheel: View? by lazy {
        exoPlayer?.findViewById(R.id.progress_bar_container)
    }

    private val backButton: MaterialButton? by lazy {
        exoPlayer?.findViewById(R.id.back_button)
    }

    private val titleLabel: MaterialTextView? by lazy {
        exoPlayer?.findViewById(R.id.title)
    }

    protected abstract val exoPlayer: StyledPlayerView?

    protected abstract val motionLayout: ConstraintLayout?

    protected abstract val minimalLayout: View?

    protected abstract val minimalProgress: View?

    protected abstract val minimalPlayPause: View?

    protected abstract val minimalTitleTv: TextView?

    protected abstract val channelListRecyclerView: View?

    protected abstract val exitButton: View?

    private val title = MutableStateFlow("")
    private val isProgressing = MutableStateFlow<Boolean>(false)
    protected val isShowChannelList = MutableStateFlow(false)

    protected abstract val playbackViewModel: BasePlaybackInteractor

    private val gestureDetector by lazy {
        object: OnSwipeTouchListener(requireContext()) {
            override fun onSwipeTop() {
                onSwipeUp()
            }

            override fun onSwipeBottom() {
                onSwipeDown()
            }

            override fun onFling() {
                exoPlayer?.controllerShowTimeoutMs = 0
            }
        }
    }

    private var playerListener: Player.Listener = object: Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == ExoPlayer.STATE_READY) {
                isProgressing.value = false
                exoPlayer?.keepScreenOn = true
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                exoPlayer?.keepScreenOn = false
            }
        }

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
    }


    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView:")
        exoPlayer?.apply {
            player = exoPlayerManager.exoPlayer
            showController()
            player?.stop()

            setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener {
                when(it) {
                    View.VISIBLE -> channelListRecyclerView?.visibility = it
                    View.GONE, View.INVISIBLE -> channelListRecyclerView?.visibility = View.INVISIBLE
                }
            })
        }
        playPauseButton?.visibility = View.INVISIBLE
        progressWheel?.visibility = View.INVISIBLE

        fullScreenButton?.visibility = View.VISIBLE
        fullScreenButton?.setOnClickListener {
            callback?.onOpenFullScreen()
        }

    }

    open fun onSwipeUp() {
        lifecycleScope.launch {
            isShowChannelList.emit(true)
        }
    }

    open fun onSwipeDown() {
        lifecycleScope.launch {
            isShowChannelList.emit(false)
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

        merge(backButton?.clicks() ?: emptyFlow(), exitButton?.clicks() ?: emptyFlow())
            .debounce(250)
            .onEach {
                exoPlayerManager.exoPlayer?.stop()
                callback?.onExitMinimal()

            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launchWhenCreated {
            title.collect {
                Log.d(TAG, "initAction: state title $it ${this@BasePlaybackFragment}")
                titleLabel?.text = it
                minimalTitleTv?.text = it
            }
        }

        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf(
            {
                playbackViewModel.playbackState.collectLatest {
                    when (it) {
                        PlaybackState.Fullscreen -> changeFullScreenLayout()
                        PlaybackState.Minimal -> changeMinimalLayout()
                        else -> {
                            exoPlayer?.keepScreenOn = false
                        }
                    }
                }
            },
            {
                isShowChannelList.collectLatest {
                    if (playbackViewModel.playbackState.value != PlaybackState.Fullscreen) return@collectLatest
                    if (it) showChannelList() else changeFullScreenLayout(shouldRedraw = false)
                }
            },{
                isProgressing.collectLatest {
                    Log.d(TAG, "initAction: isProgressing $it")
                    toggleProgressingUI(it)
                }
            }
        ))

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playbackViewModel.isInPipMode.collectLatest {
                    togglePIPLayout(it)
                }
            }
        }
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
        exoPlayer?.keepScreenOn = false
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
        exoPlayer?.keepScreenOn = true
    }

    private fun onError(throwable: Throwable?) {
        val errorCode = (throwable as? PlaybackThrowable)?.code ?: -1
        showErrorDialog(
            titleText = "Lỗi phát video",
            content = "Xin lỗi, mở nội dung không thành công. Vui lòng thử lại sau.\nMã lỗi: $errorCode",
            cancellable = false,
            onDismissListener = {
                backButton?.performClick()
            })
    }

    protected open fun onRedraw() { }

    private fun togglePIPLayout(isInPIPMode: Boolean) {
        if (isInPIPMode) {
            exoPlayer?.useController = false
            safeLet(motionLayout, inPIPModeLayout()) { constraintLayout, constraintSet ->
                performTransition(constraintLayout, constraintSet)
            }
        } else {
            changeFullScreenLayout(true)
        }
    }

    private fun toggleProgressingUI(isProgressing: Boolean) {
        if (isProgressing) {
            playPauseButton?.visibility = View.GONE
            progressWheel?.visibility = View.VISIBLE
            minimalProgress?.visibility = View.VISIBLE
            minimalPlayPause?.visibility = View.GONE
        } else {
            playPauseButton?.visibility = View.VISIBLE
            progressWheel?.visibility = View.GONE
            minimalProgress?.visibility = View.GONE
            minimalPlayPause?.visibility = View.VISIBLE
        }
    }
    protected fun changeFullScreenLayout(shouldRedraw: Boolean = true) {
        exoPlayer?.apply {
            useController = true
            controllerShowTimeoutMs = 1000
            controllerHideOnTouch = true
        }

        safeLet(motionLayout, exoPlayer, minimalLayout, channelListRecyclerView ) {
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
                setVisibility(list.id, View.VISIBLE)
                matchParentWidth(list.id)
                connect(list.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                constrainHeight(list.id, ConstraintSet.WRAP_CONTENT)
                setMargin(list.id, ConstraintSet.TOP, (-88).dpToPx())
            }, onEnd = {
                if (!playbackViewModel.isInPipMode.value) {
                    exoPlayer?.showController()
                    isShowChannelList.value = false
                } else {
                    togglePIPLayout(true)
                }
            }, onStart = {
                if (shouldRedraw) {
                    onRedraw()
                }
            })

        }
    }

    abstract fun provideMinimalLayout(): ConstraintSet?
    private fun changeMinimalLayout() {
        exoPlayer?.apply {
            useController = false
            hideController()
        }
        safeLet(motionLayout, provideMinimalLayout()) {
                mainLayout, constraintSet ->
            performTransition(mainLayout, constraintSet)
            isShowChannelList.value = false
        }
    }

    open fun inPIPModeLayout(): ConstraintSet? {
        return safeLet(exoPlayer, minimalLayout, channelListRecyclerView ) {
                exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(this)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                fillParent(exoplayer.id)

                setVisibility(minimal.id, View.GONE)
                setVisibility(list.id, View.GONE)
            }
        }
    }

    open fun showChannelListLayout(): ConstraintSet? {
        return safeLet(exoPlayer, channelListRecyclerView) {
                exoplayer, list -> ConstraintSet().apply {
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
           }
        }
    }

    protected fun showChannelList() {
        exoPlayer?.apply {
            showController()
            controllerShowTimeoutMs = -1
            controllerHideOnTouch = false
        }
        safeLet(motionLayout, showChannelListLayout()) {
                mainLayout, constraintSet ->
            performTransition(mainLayout, constraintSet)
        }
    }
    protected open suspend fun preparePlayView(data: PrepareStreamLinkData) {
        exoPlayerManager.exoPlayer?.stop()
        isProgressing.emit(true)
        title.emit(data.title)
    }

    protected open suspend fun playVideo(data: StreamLinkData) {
        exoPlayerManager.playVideo(data.linkStream, data.isHls, data.itemMetaData , playerListener)
        exoPlayer?.player = exoPlayerManager.exoPlayer
        title.emit(data.title)
    }

    private fun performTransition(layout: ConstraintLayout, set: ConstraintSet, onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null) {
        TransitionManager.beginDelayedTransition(layout, Fade().apply {
            interpolator = AccelerateInterpolator()
            duration = 500
            addListener(object: TransitionCallback() {
                override fun onTransitionStart(transition: Transition) {
                    super.onTransitionStart(transition)
                    onStart?.invoke()
                }

                override fun onTransitionEnd(transition: Transition) {
                    super.onTransitionEnd(transition)
                    onEnd?.invoke()
                }
            })
        })
        set.applyTo(layout)
    }
}

package com.kt.apps.media.mobile.ui.fragments.playback

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.complex.TransitionCallback
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log

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

sealed class LayoutState {
    data class FULLSCREEN(val shouldRedraw: Boolean): LayoutState()
    object MINIMAL: LayoutState()
    object PIP: LayoutState()
    object SHOW_CHANNEL: LayoutState()
    object MOVE_CHANNEL: LayoutState()
}

data class ScreenState(val playbackState: PlaybackState, val isInPIPMode: Boolean)
abstract class BasePlaybackFragment<T : ViewDataBinding> : BaseMobileFragment<T>(), IPlaybackControl, Player.Listener {

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

    protected val progressBar: SeekBar? by lazy {
        exoPlayer?.findViewById(R.id.exo_progress_bar)
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

    protected val channelListRecyclerView: RecyclerView? by lazy {
        exoPlayer?.findViewById(R.id.exo_channel_list)
    }

    protected val liveLabel: LinearLayout? by lazy {
        exoPlayer?.findViewById(R.id.tv_live_label)
    }

    protected val categoryLabel: MaterialTextView? by lazy {
        exoPlayer?.findViewById(R.id.category_tv)
    }

    protected abstract val exoPlayer: StyledPlayerView?

    protected abstract val motionLayout: ConstraintLayout?

    protected abstract val minimalLayout: View?

    protected abstract val minimalProgress: View?

    protected abstract val minimalPlayPause: View?

    protected abstract val minimalTitleTv: TextView?



    protected abstract val exitButton: View?

    private val currentLayout = MutableStateFlow<LayoutState>(LayoutState.FULLSCREEN(true))
    private val title = MutableStateFlow("")
    private val isProgressing = MutableStateFlow(true)
    protected val isPlayingState = MutableStateFlow(false)

    protected var retryTimes: Int = 3

    protected abstract val playbackViewModel: BasePlaybackInteractor

    private val marginBottomSize by lazy {
        ( resources.getDimensionPixelSize(R.dimen.item_channel_height) + resources.getDimensionPixelSize(R.dimen.item_channel_decoration)) * 2 / 3
    }

    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView:")
        liveLabel?.visibility = View.GONE
        exoPlayer?.apply {
            player = exoPlayerManager.exoPlayer
            showController()
            player?.stop()

            if (isLandscape) {
                setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener {
                    when(it) {
                        View.VISIBLE -> channelListRecyclerView?.visibility = it
                        View.GONE, View.INVISIBLE -> channelListRecyclerView?.visibility = View.INVISIBLE
                    }
                })
            }
        }
        playPauseButton?.visibility = View.INVISIBLE
        progressWheel?.visibility = View.INVISIBLE

        progressBar?.isEnabled = false

        fullScreenButton?.visibility = View.VISIBLE
        fullScreenButton?.setOnClickListener {
            callback?.onOpenFullScreen()
        }

        channelListRecyclerView?.visibility = View.VISIBLE
        channelListRecyclerView?.addOnScrollListener(object: OnScrollListener() {
            var isChanged: Boolean = false
            var showTimeout: Int = 1000
            var hideOnTouch: Boolean = true
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when(newState) {
                    SCROLL_STATE_IDLE -> {
                        Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_IDLE")
                        if (isChanged) {
                            exoPlayer?.apply {
                                controllerShowTimeoutMs = showTimeout
                                controllerHideOnTouch = hideOnTouch
                            }
                            isChanged = false
                        }
                    }
                    SCROLL_STATE_SETTLING -> { Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_SETTLING") }
                    SCROLL_STATE_DRAGGING -> {
                        Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_DRAGGING")
                        if (!isChanged) {
                            exoPlayer?.apply {
                                showTimeout = controllerShowTimeoutMs
                                hideOnTouch = controllerHideOnTouch

                                controllerShowTimeoutMs = -1
                                controllerHideOnTouch = false
                            }
                            isChanged = true
                        }
                    }
                }
            }
        })
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

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                merge(backButton?.clicks() ?: emptyFlow(), exitButton?.clicks() ?: emptyFlow())
                    .collectLatest {
                        delay(250)
                        exoPlayerManager.exoPlayer?.stop()
                        callback?.onExitMinimal()
                    }
            }

            launch {
                title.collect {
                    Log.d(TAG, "initAction: state title $it ${this@BasePlaybackFragment}")
                    titleLabel?.text = it
                    minimalTitleTv?.text = it
                }
            }

            launch {
                isProgressing.collectLatest {
                    Log.d(TAG, "initAction: isProgressing $it")
                    toggleProgressingUI(it)
                }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                playbackViewModel.state
                    .collectLatest { state ->
                        Log.d(TAG, "initAction: state $state ${this@BasePlaybackFragment}")
                        when(state) {
                            is PlaybackViewModel.State.LOADING -> preparePlayView(state.data)
                            is PlaybackViewModel.State.PLAYING -> {
                                retryTimes = 3
                                playVideo(state.data)
                            }
                            is PlaybackViewModel.State.ERROR -> onError(state.error)
                            else -> {}
                        }
                    }
            }

            launch {
                combine(
                    playbackViewModel.playbackState,
                    playbackViewModel.isInPipMode,
                ) { playbackState, isInPipMode ->
                    ScreenState(playbackState, isInPipMode)
                }.collectLatest { state ->
                    if (state.isInPIPMode) {
                        currentLayout.emit(LayoutState.PIP)
                    } else {
                        when(state.playbackState) {
                            PlaybackState.Fullscreen -> {
                                currentLayout.emit(LayoutState.FULLSCREEN(shouldRedraw = false))
//                                when(state.channelListState) {
//                                    ChannelListState.SHOW -> currentLayout.emit(LayoutState.SHOW_CHANNEL)
//                                    ChannelListState.MOVING -> currentLayout.emit(LayoutState.MOVE_CHANNEL)
//                                    ChannelListState.HIDE -> currentLayout.emit(LayoutState.FULLSCREEN(shouldRedraw = false))
//                                }
                            }
                            PlaybackState.Minimal -> currentLayout.emit(LayoutState.MINIMAL)
                            else -> {
                                exoPlayer?.keepScreenOn = false
                            }
                        }
                    }
                }
            }

            launch {
                currentLayout.collectLatest {
                    when(it) {
                        is LayoutState.FULLSCREEN -> changeFullScreenLayout(it.shouldRedraw)
                        is LayoutState.SHOW_CHANNEL -> changeFullScreenLayout(false)
                        is LayoutState.PIP -> togglePIPLayout(true)
                        is LayoutState.MINIMAL -> changeMinimalLayout()
                        is LayoutState.MOVE_CHANNEL -> { }
                    }
                }
            }
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
        exoPlayerManager.detach(this)
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

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        isPlayingState.value = isPlaying
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            isProgressing.value = false
            exoPlayer?.keepScreenOn = true
            progressBar?.isEnabled = true
        } else {
            progressBar?.isEnabled = false
        }
        if (playbackState == ExoPlayer.STATE_ENDED) {
            exoPlayer?.keepScreenOn = false
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.d(TAG, "onPlayerError: $error")
        if (retryTimes > 0) {
            playbackViewModel.state.replayCache.lastOrNull()
                ?.let { it as? PlaybackViewModel.State.PLAYING }
                ?.run {
                    retryTimes -= 1
                    lifecycleScope.launch {
                        playVideo(this@run.data)
                    }
                } ?: kotlin.run {
                lifecycleScope.launch {
                    playbackViewModel.playbackError(PlaybackThrowable(error.errorCode, error))
                }
            }
        } else {
            lifecycleScope.launch {
                playbackViewModel.playbackError(PlaybackThrowable(error.errorCode, error))
            }
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        Log.d(TAG, "onVideoSizeChanged: $videoSize")
    }

    private fun togglePIPLayout(isInPIPMode: Boolean) {

        if (isInPIPMode) {
            exoPlayer?.useController = false
            safeLet(motionLayout, provideInPIPModeLayout()) { constraintLayout, constraintSet ->
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
    private fun changeFullScreenLayout(shouldRedraw: Boolean = true) {
        exoPlayer?.apply {
            useController = true
            controllerShowTimeoutMs = 1000
            controllerHideOnTouch = true
            channelListRecyclerView?.visibility = View.VISIBLE
        }
        safeLet(motionLayout, provideFullScreenLayout()) {
            layout, constrainSet ->
            performTransition(layout, constrainSet, transition = AutoTransition(), onEnd = {
                if (shouldRedraw) {
                    onRedraw()
                }
            })
        }
    }


    private fun changeMinimalLayout() {
        if (isLandscape) {
            exoPlayer?.apply {
                hideController()
                useController = false
            }
        } else {
            exoPlayer?.apply {
                useController = true
                showController()
                controllerHideOnTouch = true
                controllerShowTimeoutMs = 1000
            }
        }

        safeLet(motionLayout, provideMinimalLayout()) {
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
        if (exoPlayerManager.exoPlayer?.isPlaying == true) {
            return
        }
        exoPlayerManager.playVideo(data.linkStream, data.isHls, data.itemMetaData , this)
        exoPlayer?.player = exoPlayerManager.exoPlayer
        title.emit(data.title)
    }

    private fun performTransition(layout: ConstraintLayout, set: ConstraintSet, transition: Transition = Fade(), onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null) {
        Log.d(TAG, "performTransition: ${set.TAG}")
        set.applyTo(layout)
    }

    open fun provideMinimalLayout(): ConstraintSet? {
        Log.d(TAG, "provideMinimalLayout: ")
        if (isLandscape) {
            return safeLet(
                motionLayout,
                exoPlayer,
                minimalLayout,
                channelListRecyclerView
            ) { mainLayout, exoplayer, minimal, list ->
                ConstraintSet().apply {
                    clone(mainLayout)
                    arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                        clear(it)
                    }

                    setVisibility(list.id, View.GONE)
                    matchParentWidth(list.id)
                    matchParentWidth(minimal.id)
                    matchParentWidth(exoplayer.id)
                    constrainHeight(minimal.id, ConstraintSet.WRAP_CONTENT)
                    connect(exoplayer.id, ConstraintSet.BOTTOM, minimal.id, ConstraintSet.TOP)
                    alignParent(minimal.id, ConstraintSet.BOTTOM)
                    alignParent(exoplayer.id, ConstraintSet.TOP)
                }
            }
        }
        return safeLet(
            motionLayout, exoPlayer, channelListRecyclerView
        ) { motionLayout, exoPlayer, list ->
            ConstraintSet().apply {
                clone(motionLayout)
                arrayListOf(exoPlayer.id, list.id).forEach {
                    clear(it)
                }
                fillParent(exoPlayer.id)
                setVisibility(list.id, View.GONE)
            }
        }
    }

    open fun provideFullScreenLayout(): ConstraintSet? {
        Log.d(TAG, "provideFullScreenLayout: ")
        if (isLandscape) {
            return safeLet(exoPlayer, minimalLayout, channelListRecyclerView ) {
                    exoplayer,  minimal, list ->
                ConstraintSet().apply {
                    arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                        clear(it)
                    }

                    arrayListOf(ConstraintSet.END, ConstraintSet.BOTTOM, ConstraintSet.START, ConstraintSet.TOP).forEach {
                        connect(exoplayer.id, it, ConstraintSet.PARENT_ID, it)
                    }
                    setVisibility(minimal.id, View.GONE)
                    setVisibility(list.id, View.VISIBLE)
                    matchParentWidth(list.id)
                    connect(list.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                    setMargin(list.id, ConstraintSet.BOTTOM, -marginBottomSize)
                    constrainHeight(list.id, ConstraintSet.WRAP_CONTENT)

                }
            }
        }
        return safeLet(exoPlayer, channelListRecyclerView ) {
                exoplayer,  list ->
            ConstraintSet().apply {
                arrayListOf(exoplayer.id).forEach {
                    clear(it)
                }

                fillParent(exoplayer.id)
            }
        }
    }

    open fun provideInPIPModeLayout(): ConstraintSet? {
        Log.d(TAG, "provideInPIPModeLayout: ")
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
}

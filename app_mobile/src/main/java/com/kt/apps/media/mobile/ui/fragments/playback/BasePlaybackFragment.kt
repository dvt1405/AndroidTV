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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
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

sealed class DisplayMode {
    object IDLE: DisplayMode()
    data class FULLSCREEN(val shouldRedraw: Boolean): DisplayMode()
    object MINIMAL: DisplayMode()
    object PIP: DisplayMode()

    companion object {
        fun fromPlaybackState(state: PlaybackState) : DisplayMode {
            return when(state) {
                PlaybackState.Fullscreen -> FULLSCREEN(true)
                PlaybackState.Minimal -> MINIMAL
                PlaybackState.Invisible -> IDLE
            }
        }
    }
}

enum class ChannelListState {
    SHOW, HIDE, MOVING
}
abstract class BasePlaybackFragment<T : ViewDataBinding> : BaseMobileFragment<T>(), IDispatchTouchListener, IPlaybackControl {

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

    private val displayMode = MutableStateFlow<DisplayMode>(DisplayMode.IDLE)
    private val title = MutableStateFlow("")
    private val isProgressing = MutableStateFlow(true)
    protected val isShowChannelList = MutableStateFlow(ChannelListState.HIDE)

    protected var retryTimes: Int = 3

    protected abstract val playbackViewModel: BasePlaybackInteractor

    private val marginBottomSize by lazy {
        ( resources.getDimensionPixelSize(R.dimen.item_channel_height)
                        + resources.getDimensionPixelSize(R.dimen.item_channel_decoration)) * 2 / 3
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
    }


    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView:")
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

        fullScreenButton?.visibility = View.VISIBLE
        fullScreenButton?.setOnClickListener {
            callback?.onOpenFullScreen()
        }

        channelListRecyclerView?.visibility = View.VISIBLE
        channelListRecyclerView?.setOnTouchListener { view, motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    Log.d(TAG, "channelListRecyclerView: ACTION_MOVE $motionEvent")
                    avoidTouchGesture.set(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "channelListRecyclerView: ACTION_UP $motionEvent")
                    avoidTouchGesture.set(false)
                }
                else -> { }
            }
            false
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
                playbackViewModel.playbackState.collectLatest {
                    when (it) {
                        PlaybackState.Fullscreen -> displayMode.emit(DisplayMode.FULLSCREEN(true))
                        PlaybackState.Minimal -> displayMode.emit(DisplayMode.MINIMAL)
                        else -> {
                            exoPlayer?.keepScreenOn = false
                        }
                    }
                }
            }

            launch {
                playbackViewModel.isInPipMode.collectLatest {
                    if (it) {
                        displayMode.emit(DisplayMode.PIP)
                    } else {
                        displayMode.emit(DisplayMode.fromPlaybackState(playbackViewModel.playbackState.value))
                    }
                }
            }

            launch {
                combine(displayMode, isShowChannelList) {
                    displayMode, isShowChannelList -> Pair(displayMode, isShowChannelList)
                }.collectLatest {
                    when(it.first) {
                        is DisplayMode.FULLSCREEN -> changeFullScreenLayout((it.first as DisplayMode.FULLSCREEN).shouldRedraw)
                        is DisplayMode.MINIMAL -> changeMinimalLayout()
                        is DisplayMode.PIP -> togglePIPLayout(true)
                        else -> { }
                    }
                    if (it.first !is DisplayMode.FULLSCREEN) return@collectLatest
                    if (it.second == ChannelListState.SHOW) showChannelList()
                }
            }
        }
    }

    var lastPointF = PointF(0f, 0f)
    var startPointF = PointF(0f, 0f)
    var avoidTouchGesture = AtomicBoolean(false)
    override fun onDispatchTouchEvent(view: View?, mv: MotionEvent) {
        when(mv.actionMasked) {
            MotionEvent.ACTION_DOWN ->  {
                lastPointF = PointF(mv.rawX, mv.rawY)
                startPointF = PointF(mv.rawX, mv.rawY)
            }
            MotionEvent.ACTION_MOVE -> {
                if (avoidTouchGesture.get()) {
                    return
                }
                val deltaY = mv.rawY - lastPointF.y
                val deltaX = mv.rawX - lastPointF.x
                Log.d(TAG, "onDispatchTouchEvent: $deltaX $deltaY")
                lastPointF = PointF(mv.rawX, mv.rawY)
                moveChannelList(-deltaY.toInt())
            }
            MotionEvent.ACTION_UP -> {
                val deltaY = mv.rawY - startPointF.y
                val deltaX = mv.rawX - startPointF.x
                if (deltaY >= 1 || deltaX >= 1) {
                    onTouchEnd()
                }

                lastPointF = PointF(0f, 0f)
                startPointF = PointF(0f, 0f)
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
            performTransition(layout, constrainSet, onEnd = {
                if (shouldRedraw) {
                    onRedraw()
                }
            })
        }
    }


    private fun changeMinimalLayout() {
        if (isLandscape) {
            exoPlayer?.apply {
                useController = false
                hideController()
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
            isShowChannelList.value = ChannelListState.HIDE
        }
    }
    protected fun showChannelList() {
        if (isLandscape) {
            exoPlayer?.apply {
                showController()
                controllerShowTimeoutMs = -1
                controllerHideOnTouch = false
            }
            safeLet(motionLayout, showChannelListLayout()) {
                    mainLayout, constraintSet ->
                performTransition(mainLayout, constraintSet, transition = AutoTransition())
            }
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
        exoPlayerManager.playVideo(data.linkStream, data.isHls, data.itemMetaData , playerListener)
        exoPlayer?.player = exoPlayerManager.exoPlayer
        title.emit(data.title)
    }


    private fun onTouchEnd() {
        if (!isLandscape) { return }
        val channelList = channelListRecyclerView ?: return

        if (abs(channelList.marginBottom) <= abs(marginBottomSize) / 2) {
            lifecycleScope.launch {
                isShowChannelList.emit(ChannelListState.SHOW)
            }
        } else {
            lifecycleScope.launch {
                isShowChannelList.emit(ChannelListState.HIDE)
            }
        }
    }
    private fun moveChannelList(value: Int) {
        if (!isLandscape) { return }
        val channelList = channelListRecyclerView ?: return
        if (channelList.marginBottom + value > 0 || abs(channelList.marginBottom + value) > abs(marginBottomSize)) {
            return
        }
        exoPlayer?.apply {
            showController()
            controllerShowTimeoutMs = -1
            controllerHideOnTouch = fals
        isShowChannelList.value = ChannelListState.MOVING
        safeLet(motionLayout, moveChannelListLayout(value)) {
            layout, constraint ->
            constraint.applyTo(layout)
        }
    }
    private fun performTransition(layout: ConstraintLayout, set: ConstraintSet, transition: Transition = Fade(), onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null) {
        Log.d(TAG, "performTransition:")
        TransitionManager.beginDelayedTransition(layout, transition.apply {
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

    open fun provideMinimalLayout(): ConstraintSet? {
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
                    alignParent(exoplayer.id, it)
                }
                connect(exoplayer.id, ConstraintSet.BOTTOM, list.id, ConstraintSet.TOP, (-88).dpToPx())
                clear(list.id)
                constrainHeight(list.id, ConstraintSet.WRAP_CONTENT)
                matchParentWidth(list.id)
                alignParent(list.id, ConstraintSet.BOTTOM, (-20).dpToPx())
            }
        }
    }

    open fun moveChannelListLayout(value: Int): ConstraintSet? {
        return safeLet(motionLayout, exoPlayer, channelListRecyclerView) {
                layout, exoplayer, list -> ConstraintSet().apply {
            clone(layout)
            val currentMargin = getConstraint(list.id).layout.bottomMargin
            Log.d(TAG, "moveChannelListLayout: $currentMargin")
            setMargin(list.id, ConstraintSet.BOTTOM, currentMargin + value)
        }
        }
    }


}

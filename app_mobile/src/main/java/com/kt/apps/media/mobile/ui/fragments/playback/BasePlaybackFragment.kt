package com.kt.apps.media.mobile.ui.fragments.playback

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.allViews
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.Transition
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.inVisible
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.models.PlaybackThrowable
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadFavorite
import com.kt.apps.media.mobile.viewmodels.features.toggleFavoriteCurrent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.w3c.dom.Text
import java.util.Formatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

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

    protected val subTitle: TextView? by lazy {
        exoPlayer?.findViewById(R.id.sub_title)
    }

    private val playPauseButton: ImageButton? by lazy {
        exoPlayer?.findViewById(R.id.exo_play_pause)
    }

    private val progressWheel: View? by lazy {
        exoPlayer?.findViewById(R.id.progress_bar_container)
    }

    private val backButton: MaterialButton? by lazy {
        exoPlayer?.findViewById(R.id.back_button)
    }

    private val favoriteButton: MaterialButton? by lazy {
        exoPlayer?.findViewById(R.id.favorite_button)
    }

    private val informationButton: MaterialButton? by lazy {
        exoPlayer?.findViewById(R.id.information_button)
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

    protected val aspectRatioButton: ImageButton? by lazy {
        exoPlayer?.findViewById(R.id.exo_ratio)
    }

    protected abstract val exoPlayer: StyledPlayerView?

    protected abstract val motionLayout: ConstraintLayout?

    protected abstract val minimalLayout: View?

    protected abstract val minimalProgress: View?

    protected abstract val minimalPlayPause: View?

    protected abstract val minimalTitleTv: TextView?

    protected abstract val exitButton: View?

    private val currentLayout = MutableStateFlow<LayoutState>(LayoutState.FULLSCREEN(true))
    protected val title = MutableStateFlow("")
    protected val isProgressing = MutableStateFlow(true)
    protected val isPlayingState = MutableStateFlow(false)
    protected val currentPlayingMediaItem = MutableStateFlow<MediaItem?>(null)
    protected var retryTimes: Int = 3
    protected var executingIndex: Int = -1

    protected abstract val interactor: BasePlaybackInteractor

    private val marginBottomSize by lazy {
        ( resources.getDimensionPixelSize(R.dimen.item_channel_height) + resources.getDimensionPixelSize(R.dimen.item_channel_decoration)) * 2 / 3
    }

    protected var lastPlayerControllerConfig: PlayerControllerConfig = PlayerControllerConfig(true, 3000)

    private lateinit var informationDialog: AlertDialog

    private val channelListVisibility: Int
        get() {
            return if (context?.resources?.getBoolean(R.bool.is_small_size) != false) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

    private val exceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
        showDefaultErrorDialog()
    }

    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView:")
        liveLabel?.visibility = View.GONE
        exoPlayer?.apply {
            player = exoPlayerManager.exoPlayer
            lastPlayerControllerConfig = PlayerControllerConfig(controllerHideOnTouch, controllerShowTimeoutMs)
            showController()
            player?.stop()

            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            if (isLandscape) {
                setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener {
                    toggleChannelListVisibility(shouldShow = it == View.VISIBLE)
                })
            }
        }

        favoriteButton?.icon = ResourcesCompat.getDrawable(resources, com.kt.apps.resources.R.drawable.ic_bookmark_selector, context?.theme)
        favoriteButton?.isSelected = false
        favoriteButton?.setOnClickListener { view ->
            Log.d(TAG, "toggleFavorite: ")
            toggleFavorite()
        }
        favoriteButton?.inVisible()

        informationButton?.icon = ResourcesCompat.getDrawable(resources, com.kt.apps.core.R.drawable.ic_round_error_outline_24, context?.theme)
        informationButton?.setOnClickListener { showInformationDialog() }
        informationButton?.inVisible()

        playPauseButton?.visibility = View.INVISIBLE
        progressWheel?.visibility = View.INVISIBLE

        progressBar?.isEnabled = false

        backButton?.icon = ResourcesCompat.getDrawable(resources, if (isLandscape) {
            R.drawable.ic_arrow_back
        } else {
            R.drawable.ic_clear
        }, context?.theme)

        fullScreenButton?.visibility = View.VISIBLE
        fullScreenButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.exo_ic_fullscreen_exit, context?.theme))

        aspectRatioButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_aspect_ratio, context?.theme))
        aspectRatioButton?.setOnClickListener { changeNextResizeMode() }
        aspectRatioButton?.visibility = if (isLandscape) {
            View.VISIBLE
        } else {
            View.GONE
        }

        toggleChannelListVisibility(shouldShow = true)
        channelListRecyclerView?.addOnScrollListener(object: OnScrollListener() {
            var isChanged: Boolean = false
            var baseConfig = lastPlayerControllerConfig

            fun avoidChangeHide() {
                val exoPlayer = exoPlayer ?: return
                val shouldConfig = exoPlayer.controllerHideOnTouch && exoPlayer.controllerShowTimeoutMs > 0
                if (!shouldConfig) { return }
                exoPlayer.apply {
                    baseConfig = PlayerControllerConfig(hideOnTouch = controllerHideOnTouch, controllerShowTimeoutMs)

                    controllerShowTimeoutMs = -1
                    controllerHideOnTouch = false
                }
                isChanged = true
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when(newState) {
                    SCROLL_STATE_IDLE -> {
                        Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_IDLE")
                        if (isChanged) {
                            exoPlayer?.apply {
                                controllerShowTimeoutMs = baseConfig.showTimeout
                                controllerHideOnTouch = baseConfig.hideOnTouch
                            }
                            isChanged = false
                        }
                    }
                    SCROLL_STATE_SETTLING -> {
                        Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_SETTLING")
                        avoidChangeHide()
                    }
                    SCROLL_STATE_DRAGGING -> {
                        Log.d(TAG, "onScrollStateChanged: SCROLL_STATE_DRAGGING")
                        avoidChangeHide()
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

        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                merge(
                    if (!isLandscape)
                        backButton?.clicks()  ?: emptyFlow()
                    else emptyFlow(),
                    exitButton?.clicks() ?: emptyFlow())
                    .collectLatest {
                        delay(250)
                        exit()
                    }
            }

            launch {
                merge(
                    if (isLandscape)
                        backButton?.clicks()  ?: emptyFlow()
                    else emptyFlow(),
                    fullScreenButton?.clicks() ?: emptyFlow())
                    .collectLatest {
                        changeToFullscreen()
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
                    Log.d(TAG, "Playback: isProgressing $it")
                    toggleProgressingUI(it)
                }
            }

            launch {
                isPlayingState.collectLatest {
                    if (it) {
                        isProgressing.value = false
                    }
                }
            }

            lifecycleScope.launch {
                interactor.state.shareIn(lifecycleScope, SharingStarted.WhileSubscribed(), 0)
                    .collectLatest { state ->
                        Log.d(TAG, "initAction: state $state ${this@BasePlaybackFragment}")
                        when(state) {
                            is PlaybackViewModel.State.LOADING -> preparePlayView(state.data)
                            is PlaybackViewModel.State.PLAYING -> {
                                retryTimes = MAX_RETRY_TIME
                                executingIndex = -1
                                playVideo(state.data)
                            }
                            is PlaybackViewModel.State.ERROR -> onError(state.error)
                            else -> {}
                        }
                    }
            }

            launch {
                interactor.currentPlayingVideo.collectLatest {
                    if (it != null) {
                        favoriteButton?.visible()
                    } else {
                        favoriteButton?.inVisible()
                    }
                }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                combine(
                    interactor.playbackState,
                    interactor.isInPipMode,
                ) { playbackState, isInPipMode ->
                    ScreenState(playbackState, isInPipMode)
                }.collectLatest { state ->
                    if (state.isInPIPMode) {
                        currentLayout.emit(LayoutState.PIP)
                    } else {
                        when(state.playbackState) {
                            PlaybackState.Fullscreen -> {
                                currentLayout.emit(LayoutState.FULLSCREEN(shouldRedraw = false))
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
                interactor.listFavorite.collectLatest {
                    Log.d(TAG, "toggleFavoriteCurrent listFavorite: ${it}")
                }
            }

            launch {
                combine(interactor.listFavorite, currentPlayingMediaItem, transform = { listFavorite, currentPlay ->
                    Log.d(TAG, "toggleFavoriteCurrent combine: ${listFavorite}")
                    listFavorite.any { videoFavoriteDTO ->
                        videoFavoriteDTO.id == currentPlay?.mediaId && videoFavoriteDTO.title == currentPlay.mediaMetadata.title
                    }
                }).collectLatest {
                    Log.d(TAG, "toggleFavoriteCurrent combine: ${it}")
                    favoriteButton?.isSelected = it
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

        interactor.loadFavorite()
    }

    private fun changeNextResizeMode() {
        val player = exoPlayer ?: return

        val currentResizeMode = player.resizeMode
        val next = RATIO_VALUES.indexOf(currentResizeMode)
            .takeIf { it != -1 }
            ?.let { RATIO_VALUES.getOrNull(it + 1) }
            ?: kotlin.run { RATIO_VALUES.first() }

        player.resizeMode = next

    }

    private suspend fun changeToFullscreen() {
        exoPlayer?.hideController()
        delay(250)
        callback?.onOpenFullScreen()
    }
    
    private fun showInformationDialog() {
        val player = exoPlayerManager.exoPlayer ?: return
        informationDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(layoutInflater.inflate(R.layout.dialog_video_info, null).apply {
                findViewById<TextView>(com.kt.apps.core.R.id.video_title).apply {
                    text = player.mediaMetadata.title
                }
                if (player.contentDuration < 120_000) {
                    findViewById<TextView>(com.kt.apps.core.R.id.video_duration)?.gone()
                    findViewById<TextView>(com.kt.apps.core.R.id.video_duration_title)?.gone()
                } else {
                    findViewById<TextView>(com.kt.apps.core.R.id.video_duration_title)?.visible()
                    findViewById<TextView>(com.kt.apps.core.R.id.video_duration)?.apply {
                        visible()
                        val builder = StringBuilder()
                        val formatter = Formatter(builder, Locale.getDefault())
                        text = Util.getStringForTime(builder, formatter, player.contentDuration)
                        setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                    }
                }
                findViewById<TextView>(com.kt.apps.core.R.id.video_resolution)?.text =
                    "${player.videoSize.width}x${player.videoSize.height}"
                findViewById<TextView>(com.kt.apps.core.R.id.color_info)?.text = "${player.videoFormat?.colorInfo ?: "NoValue"}"
                findViewById<TextView>(com.kt.apps.core.R.id.video_codec)?.text = player.videoFormat?.codecs
                findViewById<TextView>(com.kt.apps.core.R.id.video_frame_rate)?.text = "${player.videoFormat?.frameRate}"
                findViewById<TextView>(com.kt.apps.core.R.id.audio_codec)?.text = player.audioFormat?.codecs.takeIf {
                    !it?.trim().isNullOrEmpty()
                } ?: "NoValue"

                this.findTextViewsInView().forEach {
                    it.apply {
                        setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                    }
                }
            })
            .show()
    }

    private fun exit() {
        exoPlayerManager.exoPlayer?.stop()
        callback?.onExitMinimal()
    }
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
        _cachePlayingState = exoPlayerManager.exoPlayer?.isPlaying ?: false
        exoPlayerManager.pause()
        exoPlayer?.keepScreenOn = false

        if (this::informationDialog.isInitialized) {
            if (informationDialog.isShowing) {
                informationDialog.dismiss()
            }
        }
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

    protected fun coroutineError(): (CoroutineContext, Throwable) -> Unit {
        return { _, throwable ->
            onError(throwable)
        }
    }

    protected fun onError(throwable: Throwable?) {
        val errorCode = (throwable as? PlaybackThrowable)?.code ?: -1
        Log.d(TAG, "onError: ${Throwable().stackTraceToString()}")
        showErrorDialog(
            content = "Xin lỗi, mở nội dung không thành công. Vui lòng thử lại sau.\nMã lỗi: $errorCode",
            cancellable = false)
    }

    protected open fun onRedraw() { }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.d(TAG, "Playback onIsPlayingChanged: $isPlaying")
        isPlayingState.value = isPlaying
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            isProgressing.value = false
            exoPlayer?.keepScreenOn = true
            progressBar?.isEnabled = true
            informationButton?.visible()

            retryTimes = MAX_RETRY_TIME
            executingIndex = -1
        } else {
            progressBar?.isEnabled = false
            informationButton?.inVisible()
        }
        if (playbackState == ExoPlayer.STATE_ENDED) {
            exoPlayer?.keepScreenOn = false
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        isProgressing.value = isLoading
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        fun notifyError() {
            lifecycleScope.launch {
                interactor.playbackError(PlaybackThrowable(error.errorCode, error))
            }
        }
        val executingData = interactor.state.value
            .let { (it as? PlaybackViewModel.State.PLAYING)?.data }
            ?: kotlin.run {
                notifyError()
                return
            }
        if (retryTimes > 0) {
            if (executingIndex in 0 until executingData.linkStream.size) {
                val newLinkData = executingData.linkStream.subList(
                    executingIndex + 1, executingData.linkStream.size
                )
                if (newLinkData.isNotEmpty()) {
                    val newStreamLinkData = StreamLinkData.Custom(
                        title = executingData.title,
                        linkStream = newLinkData,
                        isHls = executingData.isHls,
                        streamId = executingData.streamId,
                        itemMetaData = executingData.itemMetaData
                    )
                    lifecycleScope.launch {
                        playVideo(newStreamLinkData)
                    }
                    return
                }
            }
            executingIndex = -1
            retryTimes -= 1
            lifecycleScope.launch {
                playVideo(executingData)
            }
            return
        }
        notifyError()
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
            exoPlayer?.apply {
                this.controllerHideOnTouch = false
                this.controllerShowTimeoutMs = -1
            }
        } else {
            playPauseButton?.visibility = View.VISIBLE
            progressWheel?.visibility = View.GONE
            minimalProgress?.visibility = View.GONE
            minimalPlayPause?.visibility = View.VISIBLE
            exoPlayer?.apply {
                this.controllerHideOnTouch = lastPlayerControllerConfig.hideOnTouch
                this.controllerShowTimeoutMs = lastPlayerControllerConfig.showTimeout
            }
        }
    }

    protected fun toggleFavorite() {
        lifecycleScope.launch(exceptionHandler) {
            val isSelected = favoriteButton?.isSelected ?: return@launch
            interactor.toggleFavoriteCurrent(isSelected)
        }

    }
    private fun toggleChannelListVisibility(shouldShow: Boolean) {
        channelListRecyclerView?.visibility = if (shouldShow) {
            channelListVisibility
        } else {
            View.GONE
        }
    }
    private fun changeFullScreenLayout(shouldRedraw: Boolean = true) {
        fullScreenButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.exo_ic_fullscreen_exit, context?.theme))
        exoPlayer?.apply {
            useController = true
            controllerShowTimeoutMs = lastPlayerControllerConfig.showTimeout
            controllerHideOnTouch = lastPlayerControllerConfig.hideOnTouch
            toggleChannelListVisibility(true)
            MainScope().launch {
                delay(250)
                showController()
            }
        }
        safeLet(motionLayout, provideFullScreenLayout()) {
            layout, constrainSet ->
            performTransition(layout, constrainSet, transition = AutoTransition(), onEnd = {
                if (shouldRedraw) {
                    onRedraw()
                }
            })
        }

        if (exoPlayer?.player?.isPlaying == false) {
            exoPlayer?.player?.play()
        }
    }


    private fun changeMinimalLayout() {
        fullScreenButton?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.exo_ic_fullscreen_enter, context?.theme))
        if (isLandscape) {
            exoPlayer?.apply {
                hideController()
                useController = false
            }
        } else {
            exoPlayer?.apply {
                useController = true
                showController()
                controllerHideOnTouch = lastPlayerControllerConfig.hideOnTouch
                controllerShowTimeoutMs = lastPlayerControllerConfig.showTimeout
            }
        }

        safeLet(motionLayout, provideMinimalLayout()) {
                mainLayout, constraintSet ->
            performTransition(mainLayout, constraintSet)
        }
    }
    protected open suspend fun preparePlayView(data: PrepareStreamLinkData) {
        exoPlayerManager.exoPlayer?.stop()
        exoPlayerManager.detach()
        isProgressing.emit(true)
        title.emit(data.title)
    }

    protected open suspend fun playVideo(data: StreamLinkData) {
        if (exoPlayerManager.exoPlayer?.isPlaying == true) {
            return
        }
        Log.d(TAG, "playVideo: - retry time ${retryTimes} ${executingIndex} ${data.linkStream}")
        if (executingIndex < 0) {
            executingIndex = 0
        } else {
            executingIndex += 1
        }
        exoPlayerManager.playVideo(data.linkStream, data.isHls, data.itemMetaData , this)
        exoPlayer?.player = exoPlayerManager.exoPlayer
        title.emit(data.title)
        currentPlayingMediaItem.emit(exoPlayerManager.exoPlayer?.currentMediaItem)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        currentPlayingMediaItem.value = exoPlayerManager.exoPlayer?.currentMediaItem
    }

    private fun performTransition(layout: ConstraintLayout, set: ConstraintSet, transition: Transition = Fade(), onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null) {
        Log.d(TAG, "performTransition: ${set.TAG}")
        set.applyTo(layout)
        MainScope().launch {
            delay(250)
            onEnd?.invoke()
        }
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

    data class PlayerControllerConfig(val hideOnTouch: Boolean, val showTimeout: Int)

    companion object {
        private const val MAX_RETRY_TIME = 3
        private val RATIO_VALUES_MAP = mapOf(
            Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "RESIZE_MODE_FIT"),
            Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "RESIZE_MODE_ZOOM"),
        )
        private val RATIO_VALUES = RATIO_VALUES_MAP.keys.toIntArray()
    }
}

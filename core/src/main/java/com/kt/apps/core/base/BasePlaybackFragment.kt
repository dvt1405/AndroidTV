package com.kt.apps.core.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.R
import com.kt.apps.core.base.leanback.*
import com.kt.apps.core.base.leanback.media.LeanbackPlayerAdapter
import com.kt.apps.core.base.leanback.media.PlaybackTransportControlGlue
import com.kt.apps.core.base.leanback.media.SurfaceHolderGlueHost
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.base.viewmodels.BaseFavoriteViewModel
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.*
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import java.util.Formatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.math.max


private const val MIN_SEEK_DURATION = 30 * 1000

abstract class BasePlaybackFragment : PlaybackSupportFragment(),
    HasAndroidInjector, IMediaKeycodeHandler {
    protected val progressManager by lazy {
        ProgressBarManager()
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var actionLogger: IActionLogger

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>
    protected var mAdapter: ObjectAdapter? = null
    protected var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    private var mGridPresenter: VerticalGridPresenter? = null
    private var mVideoSurface: SurfaceView? = null
    private var mMediaPlaybackCallback: SurfaceHolder.Callback? = null
    private var mState = SURFACE_NOT_CREATED
    private var mSelectedPosition = -1
    private var mPlayingPosition = -1
    private var mGridViewPickHeight = 0f
    private var mGridViewOverlays: FrameLayout? = null
    private var playPauseBtn: ImageButton? = null
    private var playbackOverlaysContainerView: View? = null
    private var mBackgroundView: View? = null
    private var playbackInfoContainerView: LinearLayout? = null
    private var playbackTitleTxtView: TextView? = null
    private var playbackInfoTxtView: TextView? = null
    private var playbackIsLiveTxtView: TextView? = null
    private var seekBar: SeekBar? = null
    private var seekBarContainer: ViewGroup? = null
    private var contentPositionView: TextView? = null
    private var contentDurationView: TextView? = null
    private var centerContainerView: View? = null
    private var videoInfoCodecContainerView: ViewGroup? = null
    private var btnVideoCodecInfo: ImageButton? = null
    private var btnFavouriteVideo: ImageButton? = null
    protected var onItemClickedListener: OnItemViewClickedListener? = null
    private val mChildLaidOutListener = OnChildLaidOutListener { _, _, _, _ ->
    }
    private val mGlueHost by lazy {
        BasePlaybackSupportFragmentGlueHost(this@BasePlaybackFragment)
    }
    abstract val numOfRowColumns: Int

    private val mainLooper by lazy {
        Handler(Looper.getMainLooper())
    }
    protected var favoriteViewModel: BaseFavoriteViewModel? = null

    private var durationSet = false
    private val mPlayerListener by lazy {
        object : Player.Listener {

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                exoPlayerManager.exoPlayer?.let {
                    setCodecInfo(it)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                onHandlePlayerError(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    playPauseBtn?.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.round_pause_24
                        )
                    )
                    if (overlaysUIState != OverlayUIState.STATE_HIDDEN
                        && overlaysUIState != OverlayUIState.STATE_ONLY_GRID_CONTENT
                    ) {
                        handleUI(OverlayUIState.STATE_INIT, true)
                    }
                    exoPlayerManager.exoPlayer?.let {
                        setCodecInfo(it)
                    }
                } else {
                    playPauseBtn?.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.round_play_arrow_24
                        )
                    )
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                exoPlayerManager.exoPlayer?.let {
                    setCodecInfo(it)
                }
                if (events.containsAny(
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED
                    )
                ) {
                    if (player.playbackState == Player.STATE_BUFFERING) {
                        progressManager.show()
                    } else {
                        if (player.playbackState != Player.STATE_IDLE) {
                            progressManager.hide()
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                onPlayerPlaybackStateChanged(playbackState)
                if (playbackState == ExoPlayer.STATE_READY) {
                    seekBar?.isActivated = true
                    changeNextFocus()
                    val player = exoPlayerManager.exoPlayer ?: return
                    durationSet = true
                    updateProgress(player)
                    exoPlayerManager.exoPlayer?.setSeekParameters(SeekParameters(10_000L, 10_000L))
                }
            }
        }
    }

    private val timerTask by lazy {
        object : TimerTask() {
            override fun run() {
                mainLooper.post {
                    if (exoPlayerManager.exoPlayer?.isPlaying == true
                        && exoPlayerManager.exoPlayer?.playbackState != Player.STATE_ENDED
                    ) {
                        val player = exoPlayerManager.exoPlayer ?: return@post
                        updateProgress(player)
                    }
                }
            }

        }
    }

    private var timer: Timer? = null

    private fun updateTimeline(player: Player) {
        updateProgress(player)
    }

    protected val formatBuilder = StringBuilder()
    protected val formatter = Formatter(formatBuilder, Locale.getDefault())
    private var contentPosition = 0L

    private fun updateProgress(player: Player) {
        val realDurationMillis: Long = player.duration
        if (seekBarContainer?.isVisible == false) {
            return
        }
        if (seekBarContainer?.isVisible == true &&
            realDurationMillis < 10_000
        ) {
            seekBarContainer?.gone()
            return
        }
        seekBar?.max = realDurationMillis.toInt()
        contentPosition = player.contentPosition
        seekBar?.setSecondaryProgress(player.bufferedPosition.toInt())
        seekBar?.progress = player.contentPosition.toInt()
        val contentPosition = "${Util.getStringForTime(formatBuilder, formatter, player.contentPosition)} /"
        val contentDuration = " ${Util.getStringForTime(formatBuilder, formatter, player.contentDuration)}"
        contentPositionView?.text = contentPosition
        contentDurationView?.text = contentDuration
    }

    private fun setCodecInfo(player: ExoPlayer) {
        view?.findViewById<TextView>(R.id.video_title)?.text = player.mediaMetadata.title
        if (player.contentPosition < 120_000) {
            view?.findViewById<TextView>(R.id.video_duration)?.gone()
            view?.findViewById<TextView>(R.id.video_duration_title)?.gone()
        } else {
            view?.findViewById<TextView>(R.id.video_duration_title)?.visible()
            view?.findViewById<TextView>(R.id.video_duration)
                ?.let {
                    it.visible()
                    it.text = Util.getStringForTime(
                        formatBuilder,
                        formatter,
                        player.contentPosition
                    )
                }
        }
        view?.findViewById<TextView>(R.id.video_resolution)?.text =
            "${player.videoSize.width}x${player.videoSize.height}"
        view?.findViewById<TextView>(R.id.color_info)?.text = "${player.videoFormat?.colorInfo ?: "NoValue"}"
        view?.findViewById<TextView>(R.id.video_codec)?.text = player.videoFormat?.codecs
        view?.findViewById<TextView>(R.id.video_frame_rate)?.text = "${player.videoFormat?.frameRate}"
        view?.findViewById<TextView>(R.id.audio_codec)?.text = player.audioFormat?.codecs
    }

    open fun onPlayerPlaybackStateChanged(playbackState: Int) {

    }
    open fun onHandlePlayerError(error: PlaybackException) {
        Logger.e(this, tag = "onHandlePlayerError", exception = error)
    }

    private var mAutoHideTimeout: Long = 0

    private val mHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Logger.d(this@BasePlaybackFragment, "HandlerUI", "${msg.what}")
            }
        }
    }
    private val autoHideOverlayRunnable by lazy {
        Runnable {
            playPauseBtn?.visible()
            playPauseBtn?.requestFocus()
            playbackOverlaysContainerView?.fadeOut {
                playPauseBtn?.alpha = 0f
                mGridViewHolder?.gridView?.selectedPosition = 0
                mGridViewOverlays?.translationY = mGridViewPickHeight
                mSelectedPosition = 0
                mGridViewHolder?.gridView?.clearFocus()
                overlaysUIState = OverlayUIState.STATE_HIDDEN
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        progressManager.initialDelay = 0
        progressManager.setRootView(root)
        mVideoSurface = LayoutInflater.from(context)
            .inflate(R.layout.core_layout_surfaces, root, false) as SurfaceView
        root.addView(mVideoSurface, 0)
        root.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Logger.d(this@BasePlaybackFragment, tag = "RootView", message = "Height: $mGridViewPickHeight")
                mGridViewPickHeight = root.height - DEFAULT_OVERLAY_PICK_HEIGHT
                mGridViewOverlays?.translationY = mGridViewPickHeight
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        mVideoSurface!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceCreated(holder)
                mState = SURFACE_CREATED
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                mMediaPlaybackCallback?.surfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceDestroyed(holder)
                mState = SURFACE_NOT_CREATED
            }
        })
        backgroundType = BG_LIGHT
        val playbackOverlayContainer = LayoutInflater.from(context)
            .inflate(R.layout.playback_vertical_grid_overlay, container, false)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        playbackOverlaysContainerView = playbackOverlayContainer
        playbackOverlayContainer.layoutParams = layoutParams
        mGridViewOverlays = playbackOverlayContainer.findViewById(R.id.browse_grid_dock)
        setupVerticalGridView(playbackOverlayContainer)
        root.addView(playbackOverlayContainer)
        videoInfoCodecContainerView = LayoutInflater.from(context)
            .inflate(R.layout.layout_video_codec_info, container, false) as ViewGroup
        root.addView(videoInfoCodecContainerView)
        mBackgroundView = root.findViewById(androidx.leanback.R.id.playback_fragment_background)
        playbackTitleTxtView = root.findViewById(R.id.playback_title)
        playbackInfoTxtView = root.findViewById(R.id.playback_info)
        playbackIsLiveTxtView = root.findViewById(R.id.playback_live)
        playbackInfoContainerView = root.findViewById(R.id.info_container)
        playPauseBtn = root.findViewById(R.id.ic_play_pause)
        seekBar = root.findViewById(R.id.video_progress_bar)
        seekBarContainer = root.findViewById(R.id.progress_bar_container)
        contentPositionView = root.findViewById(R.id.content_position)
        contentDurationView = root.findViewById(R.id.content_duration)
        btnVideoCodecInfo = root.findViewById(R.id.btn_video_codec_info)
        centerContainerView = root.findViewById(R.id.center_controls_container)
        btnFavouriteVideo = root.findViewById(R.id.btn_favourite)
        hideControlsOverlay(false)
        val controlBackground = root.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground.makeGone()
        playPauseBtn?.setOnClickListener {
            onPlayPauseIconClicked()
        }
        progressManager.setOnProgressShowHideListener { show ->
            if (show) {
                if (overlaysUIState == OverlayUIState.STATE_INIT
                    || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                ) {
                    handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
                }
            } else {
                if (overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                    || overlaysUIState == OverlayUIState.STATE_INIT
                ) {
                    btnFavouriteVideo?.fadeIn {}
                    playPauseBtn?.fadeIn {
                        focusedPlayBtnIfNotSeeking()
                        overlaysUIState = OverlayUIState.STATE_INIT
                    }
                }
            }
        }
        videoInfoCodecContainerView?.gone()
        btnVideoCodecInfo?.setOnClickListener {
            mHandler.removeCallbacks(autoHideOverlayRunnable)
            videoInfoCodecContainerView?.fadeIn {}
        }
        btnFavouriteVideo?.setOnClickListener {
            it.isSelected = !it.isSelected
            onFavoriteVideoClicked(it.isSelected)
            mHandler.removeCallbacks(autoHideOverlayRunnable)
            mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        }
        return root
    }

    protected open fun onFavoriteVideoClicked(isFavorite: Boolean) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exoPlayerManager.prepare()
        exoPlayerManager.playerAdapter?.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
        mTransportControlGlue = PlaybackTransportControlGlue(activity, exoPlayerManager.playerAdapter)
        mTransportControlGlue.host = mGlueHost
        mTransportControlGlue.isSeekEnabled = false

    }

    override fun onStart() {
        super.onStart()
        hideControlsOverlay(false)
        val controlBackground = view?.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground?.makeGone()
        if (timer == null) {
            timer = Timer()
            try {
                timer?.schedule(timerTask, 1_000, 1_000)
            } catch (_: Exception) {
                timer = null
            }
        }
    }


    private fun onPlayPauseIconClicked() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        try {
            if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
                handleUI(OverlayUIState.STATE_INIT, true)
            }
            if (true == exoPlayerManager.playerAdapter?.isPlaying) {
                exoPlayerManager.playerAdapter?.pause()
            } else {
                exoPlayerManager.playerAdapter?.play()
                mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
            }
        } catch (e: Exception) {
            Logger.e(this, exception = e)
        }
    }

    private fun setupVerticalGridView(gridView: View) {
        mGridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
            shadowEnabled = false
        }
        mGridPresenter!!.numberOfColumns = numOfRowColumns
        mGridViewHolder = mGridPresenter!!.onCreateViewHolder(mGridViewOverlays)
        mGridViewOverlays?.addView(mGridViewHolder!!.view)
        mGridPresenter?.setOnItemViewSelectedListener { _, item, _, _ ->
        }
        mGridPresenter?.setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            (mAdapter as ArrayObjectAdapter).indexOf(item)
                .takeIf {
                    it > -1
                }?.let {
                    mSelectedPosition = it
                }
            onItemClickedListener?.onItemClicked(itemViewHolder, item, rowViewHolder, row)

        }
        mGridViewHolder!!.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        mGridPresenter!!.onBindViewHolder(mGridViewHolder, mAdapter)
        gridView.makeGone()
    }

    protected fun <T> setupRowAdapter(
        objectList: Map<String, List<T>>,
        presenterSelector: PresenterSelector
    ) {
        mAdapter = ArrayObjectAdapter(ListRowPresenter())
        for ((key, value) in objectList) {
            (mAdapter as ArrayObjectAdapter)
                .add(ListRow(HeaderItem(key), ArrayObjectAdapter(presenterSelector).apply {
                    this.addAll(0, value)
                }))
        }
        updateAdapter()
    }

    protected fun <T> setupRowAdapter(
        objectList: List<T>,
        presenterSelector: PresenterSelector,
        vararg related: List<T>
    ) {
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = presenterSelector
        mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        (mAdapter as ArrayObjectAdapter).addAll(0, objectList)
        if (related.isNotEmpty()) {
            for (i in related.indices) {
                (mAdapter as ArrayObjectAdapter).addAll(0, related[i])
            }
        }
        updateAdapter()
    }

    fun updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter!!.onBindViewHolder(mGridViewHolder, mAdapter)
            setSelectedPosition(max(0, mSelectedPosition))
        }
    }

    override fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        mGridViewHolder?.gridView?.setSelectedPositionSmooth(position)
    }

    private fun changeNextFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar?.focusable = if (seekBarContainer?.visibility == View.VISIBLE) {
                View.FOCUSABLE
            } else {
                View.NOT_FOCUSABLE
            }
        }

        mGridViewOverlays?.nextFocusUpId = if (seekBarContainer?.visibility == View.VISIBLE) {
            R.id.progress_bar_container
        } else {
            R.id.ic_play_pause
        }

        playPauseBtn?.nextFocusDownId = if (seekBarContainer?.visibility == View.VISIBLE) {
            R.id.progress_bar_container
        } else {
            R.id.browse_grid_dock
        }


    }

    fun prepare(
        title: String,
        subTitle: String?,
        isLive: Boolean,
        showProgressManager: Boolean = true
    ) {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        setVideoInfo(title, subTitle, isLive)

        showAllOverlayElements(false)
        if (showProgressManager) {
            progressManager.show()
        }
        if (isLive) {
            seekBarContainer?.gone()
        } else {
            seekBarContainer?.visible()
        }
    }

    fun showAllOverlayElements(autoHide: Boolean = true) {
        if (progressManager.isShowing) {
            handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, autoHide)
        } else {
            handleUI(OverlayUIState.STATE_INIT, autoHide)
        }
    }

    private val fadeInAnimator by lazy {
        ValueAnimator.ofFloat(0f, 1f)
            .apply {
                this.duration = 300L
                this.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        ensureUIByState(overlaysUIState)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        super.onAnimationCancel(animation)
                        onAnimationEnd(animation)
                    }

                    fun ensureUIByState(state: OverlayUIState) {
                        when (state) {
                            OverlayUIState.STATE_INIT -> {
                                mGridViewHolder?.gridView?.clearFocus()
                                focusedPlayBtnIfNotSeeking()
                            }

                            OverlayUIState.STATE_HIDDEN -> {
                                mGridViewHolder?.gridView?.clearFocus()
                                playPauseBtn?.requestFocus()
                                if (true == playbackOverlaysContainerView?.isVisible) {
                                    playbackOverlaysContainerView?.fadeOut()
                                }
                            }

                            OverlayUIState.STATE_ONLY_GRID_CONTENT -> {

                            }

                            OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY -> {
                                playPauseBtn?.animate()?.cancel()
                                playPauseBtn?.alpha = 0f
                            }
                        }
                    }
                })
            }
    }

    private val overlayRootContainerAnimationUpdateListener by lazy {
        ValueAnimator.AnimatorUpdateListener {
            if (it.animatedValue == 0f) {
                playbackOverlaysContainerView?.inVisible()
            } else {
                playbackOverlaysContainerView?.visible()
            }
            playbackOverlaysContainerView?.alpha = it.animatedValue as Float
        }
    }

    private val playbackInfoAnimationUpdateListener by lazy {
        ValueAnimator.AnimatorUpdateListener {
            if (it.animatedValue == 0f) {
                playbackInfoContainerView?.gone()
            } else {
                playbackInfoContainerView?.visible()
            }
            playbackInfoContainerView?.alpha = it.animatedValue as Float
        }
    }

    private val btnPlayPauseAnimationUpdateListener by lazy {
        ValueAnimator.AnimatorUpdateListener {
            playPauseBtn?.alpha = it.animatedValue as Float
            centerContainerView?.alpha = it.animatedValue as Float
            if (it.animatedValue == 1f) {
                centerContainerView?.visible()
                playPauseBtn?.visible()
            }
        }
    }
    private var overlaysUIState: OverlayUIState = OverlayUIState.STATE_HIDDEN
    private fun handleUI(targetState: OverlayUIState, autoHide: Boolean) {
        Logger.d(
            this@BasePlaybackFragment,
            tag = "HandleUI",
            message = "currentState: $overlaysUIState," +
                    "targetState: $targetState"
        )
        val currentState = overlaysUIState
        when (targetState) {
            OverlayUIState.STATE_INIT -> {
                showOverlayPlaybackControl(currentState)
            }

            OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY -> {
                showOverlayPlaybackControlWithoutBtnPlay(currentState)
            }

            OverlayUIState.STATE_ONLY_GRID_CONTENT -> {
                Logger.d(this, message = "y = ${mGridViewOverlays?.translationY} - $mGridViewPickHeight")
                fun onlyShowGridView() {
                    playbackInfoContainerView?.fadeOut()
                    centerContainerView?.fadeOut()
                    mGridViewOverlays?.translateY(0f) {
                        mGridViewOverlays?.visible()
                        mGridViewHolder?.gridView?.requestFocus()
                    }
                }
                if (currentState == OverlayUIState.STATE_HIDDEN) {
                    playbackOverlaysContainerView?.fadeIn {
                        onlyShowGridView()
                    }
                } else if ((currentState == OverlayUIState.STATE_INIT
                            || currentState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY)
                    && fadeInAnimator.isRunning
                ) {
                    fadeInAnimator.pause()
                    fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            fadeInAnimator.setupEndValues()
                            overlayRootContainerAnimationUpdateListener.onAnimationUpdate(fadeInAnimator)
                            onlyShowGridView()
                            fadeInAnimator.removeListener(this)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            super.onAnimationCancel(animation)
                            onAnimationEnd(animation)
                        }
                    })
                    fadeInAnimator.resume()
                } else if (playbackOverlaysContainerView?.visibility != View.VISIBLE) {
                    fadeInAnimator.removeAllUpdateListeners()
                    fadeInAnimator.addUpdateListener(overlayRootContainerAnimationUpdateListener)
                    fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            onlyShowGridView()
                            overlayRootContainerAnimationUpdateListener.onAnimationUpdate(ValueAnimator.ofFloat(1f))
                            fadeInAnimator.removeListener(this)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            super.onAnimationCancel(animation)
                            onAnimationEnd(animation)
                        }
                    })
                    fadeInAnimator.start()
                } else if (playbackInfoContainerView?.visibility == View.VISIBLE) {
                    fadeInAnimator.cancel()
                    onlyShowGridView()
                }
            }

            OverlayUIState.STATE_HIDDEN -> {
                fadeInAnimator.cancel()
                fadeInAnimator.removeAllUpdateListeners()
                autoHideOverlayRunnable.run()
            }
        }

        mHandler.removeCallbacks(autoHideOverlayRunnable)
        if (autoHide) {
            mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        }
        overlaysUIState = targetState
    }

    private fun showOverlayPlaybackControl(currentState: OverlayUIState) {
        if (currentState == OverlayUIState.STATE_ONLY_GRID_CONTENT) {
            mGridViewHolder?.gridView?.scrollToPosition(0)
            focusedPlayBtnIfNotSeeking()
            fadeInAnimator.cancel()
            fadeInAnimator.removeAllUpdateListeners()
            fadeInAnimator.addUpdateListener(btnPlayPauseAnimationUpdateListener)
            fadeInAnimator.addUpdateListener(playbackInfoAnimationUpdateListener)
            fadeInAnimator.start()
            mGridViewOverlays?.translateY(mGridViewPickHeight, onAnimationEnd = {
                focusOnDpadUp()
            }, onAnimationCancel = {
                focusOnDpadUp()
                mGridViewOverlays?.translationY = mGridViewPickHeight
            })
        } else if (currentState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY) {
            if (fadeInAnimator.isRunning) {
                fadeInAnimator.pause()
                fadeInAnimator.addUpdateListener(btnPlayPauseAnimationUpdateListener)
                fadeInAnimator.resume()
            } else {
                centerContainerView?.fadeIn {}
                playPauseBtn?.fadeIn {
                    focusedPlayBtnIfNotSeeking()
                }
            }
        } else if (currentState == OverlayUIState.STATE_HIDDEN) {
            fadeInAnimator.cancel()
            fadeInAnimator.removeAllUpdateListeners()
            fadeInAnimator.addUpdateListener(overlayRootContainerAnimationUpdateListener)
            fadeInAnimator.addUpdateListener(btnPlayPauseAnimationUpdateListener)
            fadeInAnimator.addUpdateListener(playbackInfoAnimationUpdateListener)
            fadeInAnimator.start()
        }

        if (currentState != OverlayUIState.STATE_ONLY_GRID_CONTENT) {
            if (mGridViewPickHeight > 0) {
                mGridViewOverlays?.translationY = mGridViewPickHeight
            }
            focusedPlayBtnIfNotSeeking()
        }
    }

    private fun showOverlayPlaybackControlWithoutBtnPlay(currentState: OverlayUIState) {
        mGridViewHolder?.gridView?.scrollToPosition(0)
        when (currentState) {
            OverlayUIState.STATE_INIT -> {
                fadeInAnimator.removeUpdateListener(btnPlayPauseAnimationUpdateListener)
                playPauseBtn?.alpha = 0f
            }

            OverlayUIState.STATE_ONLY_GRID_CONTENT -> {
                fadeInAnimator.removeAllUpdateListeners()
                fadeInAnimator.cancel()
                fadeInAnimator.addUpdateListener(playbackInfoAnimationUpdateListener)
                fadeInAnimator.start()
                mGridViewOverlays?.translateY(mGridViewPickHeight, onAnimationEnd = {
                    playPauseBtn?.alpha = 0f
                }, onAnimationCancel = {
                    playPauseBtn?.alpha = 0f
                    mGridViewOverlays?.translationY = mGridViewPickHeight
                })
            }

            OverlayUIState.STATE_HIDDEN -> {
                fadeInAnimator.cancel()
                playPauseBtn?.alpha = 0f
                mGridViewOverlays?.translationY = mGridViewPickHeight
                fadeInAnimator.removeAllUpdateListeners()
                fadeInAnimator.addUpdateListener(overlayRootContainerAnimationUpdateListener)
                fadeInAnimator.addUpdateListener(playbackInfoAnimationUpdateListener)
                fadeInAnimator.start()
            }

            else -> {
                playPauseBtn?.animate()?.cancel()
                playPauseBtn?.alpha = 0f
            }
        }
        focusedPlayBtnIfNotSeeking()
    }

    private fun focusedPlayBtnIfNotSeeking() {
        if (seekBar?.isFocused == false) {
            playPauseBtn?.requestFocus()
        }
    }

    enum class OverlayUIState {
        STATE_INIT,
        STATE_INIT_WITHOUT_BTN_PLAY,
        STATE_ONLY_GRID_CONTENT,
        STATE_HIDDEN
    }

    fun playVideo(
        linkStreams: List<LinkStream>,
        playItemMetaData: Map<String, String>,
        headers: Map<String, String>?,
        isHls: Boolean,
        isLive: Boolean,
        forceShowVideoInfoContainer: Boolean
    ) {
        if (!progressManager.isShowing) {
            progressManager.show()
        }
        mGlueHost.setSurfaceHolderCallback(null)
        exoPlayerManager.playVideo(
            linkStreams = linkStreams,
            isHls = isHls,
            itemMetaData = playItemMetaData,
            playerListener = mPlayerListener,
            headers = headers
        )
        mTransportControlGlue = PlaybackTransportControlGlue(activity, exoPlayerManager.playerAdapter)
        mTransportControlGlue.host = mGlueHost
        mTransportControlGlue.isSeekEnabled = true
        mTransportControlGlue.playWhenPrepared()
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        if (forceShowVideoInfoContainer) {
            setVideoInfo(
                playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_TITLE],
                playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION],
                isLive
            )
            showAllOverlayElements(false)
        }
        if (isLive) {
            seekBarContainer?.gone()
        } else {
            seekBarContainer?.visible()
        }
        changeNextFocus()
        seekBar?.isActivated = false
        seekBar?.isFocusable = false
    }

    fun getBackgroundView(): View? {
        return mBackgroundView
    }

    fun hideProgressBar() {
        view?.findViewById<ProgressBar>(androidx.leanback.R.id.playback_progress)
            ?.makeGone()
    }


    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    /**
     * Adds [SurfaceHolder.Callback] to [android.view.SurfaceView].
     */
    fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
        mMediaPlaybackCallback = callback
        if (callback != null) {
            if (mState == SURFACE_CREATED) {
                mMediaPlaybackCallback?.surfaceCreated(mVideoSurface!!.holder)
            }
        }
    }

    private fun setVideoInfo(title: String?, description: String?, isLive: Boolean = false) {
        if (!playbackTitleTxtView?.text.toString().equals(title, ignoreCase = true)) {
            playbackTitleTxtView?.text = title
        }
        if (!playbackInfoTxtView?.text.toString().equals(description, ignoreCase = true)) {
            playbackInfoTxtView?.text = description?.trim()
        }
        if (playbackTitleTxtView?.isSelected != true) {
            playbackTitleTxtView?.isSelected = true
        }
        if (description == null) {
            playbackInfoTxtView?.gone()
        } else {
            playbackInfoTxtView?.visible()
        }
        if (isLive) {
            playbackIsLiveTxtView?.visible()
        } else {
            playbackIsLiveTxtView?.gone()
        }
        playbackInfoContainerView?.fadeIn()
        playbackOverlaysContainerView?.visibility = View.VISIBLE
        if (mGridViewPickHeight > 0) {
            mSelectedPosition = 0
            playPauseBtn?.requestFocus()
            mGridViewOverlays?.translateY(mGridViewPickHeight) {}
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        exoPlayerManager.exoPlayer?.let {
            setCodecInfo(it)
        }
        val screenWidth = requireView().width
        val screenHeight = requireView().height
        val p = mVideoSurface!!.layoutParams
        if (screenWidth * height > width * screenHeight) {
            p.height = screenHeight
            p.width = screenHeight * width / height
        } else {
            p.width = screenWidth
            p.height = screenWidth * height / width
        }
        mVideoSurface!!.layoutParams = p
    }

    /**
     * Returns the surface view.
     */
    fun getSurfaceView(): SurfaceView? {
        return mVideoSurface
    }

    fun canBackToMain(): Boolean {
        return overlaysUIState == OverlayUIState.STATE_HIDDEN && videoInfoCodecContainerView?.isVisible != true
    }

    fun hideOverlay() {
        if (videoInfoCodecContainerView?.isVisible == true) {
            videoInfoCodecContainerView?.fadeOut { }
            mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        } else {
            autoHideOverlayRunnable.run()
        }
    }

    override fun onDestroyView() {
        mVideoSurface = null
        mState = SURFACE_NOT_CREATED
        super.onDestroyView()
    }

    override fun onDpadCenter() {
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            handleUI(OverlayUIState.STATE_INIT, true)
        }
    }

    fun isMenuShowed(): Boolean {
        return playbackOverlaysContainerView?.visibility == View.VISIBLE
                || overlaysUIState != OverlayUIState.STATE_HIDDEN
    }

    private fun showGridMenu(): Boolean {
        if (playbackInfoContainerView == null) {
            Logger.d(
                this, "ShowGridMenu", "{" +
                        "mPlaybackInfoContainerView null" +
                        "}"
            )
            return true
        }
        val visible = playbackOverlaysContainerView?.visibility == View.VISIBLE
        Logger.d(
            this, "ShowGridMenu", "{" +
                    "mPlaybackOverlaysContainerView visibility: ${playbackOverlaysContainerView?.visibility}" +
                    "mPlaybackOverlaysContainerView alpha: ${playbackOverlaysContainerView?.alpha}" +
                    "}"
        )
        if (!visible || playbackOverlaysContainerView!!.alpha < 1f) {
            if (progressManager.isShowing) {
                handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
            } else {
                handleUI(OverlayUIState.STATE_INIT, true)
            }
        }
        return false
    }

    override fun onDpadDown() {
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            if (progressManager.isShowing) {
                handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
            } else {
                handleUI(OverlayUIState.STATE_INIT, true)
            }
        } else if (overlaysUIState == OverlayUIState.STATE_INIT
            || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
        ) {
            if ((playPauseBtn?.isFocused == true)
                && (seekBarContainer?.visibility == View.VISIBLE)
                && seekBar?.isActivated == true
            ) {
                seekBar?.requestFocus()
                mHandler.removeCallbacks(autoHideOverlayRunnable)
                mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
            } else {
                handleUI(OverlayUIState.STATE_ONLY_GRID_CONTENT, true)
            }
        } else {
            mHandler.removeCallbacks(autoHideOverlayRunnable)
            mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        }
    }

    open fun getSearchFilter(): String {
        return ""
    }

    open fun getSearchHint(): String? {
        return null
    }

    override fun onDpadUp() {
        when {
            !isMenuShowed() -> {
                showGridMenu()
            }

            seekBarContainer?.visibility == View.VISIBLE && seekBar?.isFocused == true -> {
                mHandler.removeCallbacks(autoHideOverlayRunnable)
                mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
            }

            playPauseBtn?.isFocused == true -> {
                if (overlaysUIState == OverlayUIState.STATE_INIT
                    && playPauseBtn!!.alpha > 0f
                ) {
                    playPauseBtn?.requestFocus()
                    mHandler.removeCallbacks(autoHideOverlayRunnable)
                    mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
                    return
                    playPauseBtn?.requestFocus()
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "xemtv://iptv/search?" +
                                        "filter=${getSearchFilter()}" +
                                        (getSearchHint()?.let {
                                            "&query_hint=$it"
                                        } ?: "")
                            )
                        )
                    )
                }
            }

            mGridViewHolder!!.gridView.selectedPosition in 0..4 -> {
                if (progressManager.isShowing) {
                    handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
                } else {
                    handleUI(OverlayUIState.STATE_INIT, true)
                }
            }
        }
    }

    private fun focusOnDpadUp() {
        if (seekBarContainer?.isVisible == true && seekBar?.isActivated == true) {
            seekBar?.requestFocus()
        } else {
            playPauseBtn?.requestFocus()
            mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
        }
    }

    override fun onDpadLeft() {
        if (seekBar?.isFocused == true) {
            exoPlayerManager.exoPlayer?.let {
                if (contentPosition - MIN_SEEK_DURATION >= 0) {
                    it.seekTo(contentPosition - MIN_SEEK_DURATION)
                } else if (contentPosition >= 0) {
                    it.seekTo(0L)
                }
                updateProgress(it)
            }
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
    }

    override fun onKeyCodeForward() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition + MIN_SEEK_DURATION)
    }

    override fun onKeyCodeRewind() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition - MIN_SEEK_DURATION)
    }

    override fun onDpadRight() {
        if (seekBar?.isFocused == true) {
            exoPlayerManager.exoPlayer?.let {
                if (contentPosition + MIN_SEEK_DURATION <= it.duration) {
                    it.seekTo(contentPosition + MIN_SEEK_DURATION)
                } else if (contentPosition < it.duration) {
                    it.seekTo(it.duration - contentPosition)
                }
                updateProgress(it)
            }
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
    }

    override fun onKeyCodeChannelUp() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
    }

    override fun onKeyCodeChannelDown() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, DELAY_AUTO_HIDE_OVERLAY)
    }

    override fun onKeyCodeMediaNext() {
    }

    override fun onKeyCodeVolumeDown() {
    }

    override fun onKeyCodeVolumeUp() {
    }

    override fun onKeyCodeMediaPrevious() {
    }

    override fun onKeyCodePause() {
    }

    override fun onKeyCodePlay() {
    }

    override fun onKeyCodeMenu() {
        handleUI(OverlayUIState.STATE_ONLY_GRID_CONTENT, true)

    }

    fun showErrorDialogWithErrorCode(errorCode: Int, errorMessage: String? = null, onDismiss: () -> Unit = {}) {
        if (this.isDetached || this.isHidden) {
            return
        }
        showErrorDialog(
            content = errorMessage ?: getString(
                com.kt.apps.resources.R.string.error_playback_popup_content_text,
                errorCode
            ),
            titleText = getString(com.kt.apps.resources.R.string.error_playback_popup_title_text),
            onDismissListener = {
                progressManager.hide()
                if (overlaysUIState == OverlayUIState.STATE_HIDDEN
                    || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                    || overlaysUIState == OverlayUIState.STATE_INIT
                ) {
                    handleUI(OverlayUIState.STATE_INIT, false)
                } else {
                    mHandler.removeCallbacks(autoHideOverlayRunnable)
                }
                onDismiss()
            },
            onShowListener = {
                progressManager.hide()
                if (overlaysUIState == OverlayUIState.STATE_HIDDEN
                    || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                    || overlaysUIState == OverlayUIState.STATE_INIT
                ) {
                    handleUI(OverlayUIState.STATE_INIT, false)
                } else {
                    mHandler.removeCallbacks(autoHideOverlayRunnable)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        favoriteViewModel?.getListFavorite()
        favoriteViewModel?.listFavoriteLiveData?.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                btnFavouriteVideo?.isSelected = it.data.any {
                    it.id == exoPlayerManager.exoPlayer?.currentMediaItem?.mediaId &&
                            it.title == exoPlayerManager.exoPlayer?.currentMediaItem?.mediaMetadata?.title
                }
            }
        }
        if (mGridViewPickHeight == mGridViewOverlays?.translationY) {
            playPauseBtn?.requestFocus()
        } else {
            mGridViewOverlays?.requestFocus()
        }
    }

    class BasePlaybackSupportFragmentGlueHost(
        private val mFragment: BasePlaybackFragment
    ) : PlaybackSupportFragmentGlueHost(mFragment), SurfaceHolderGlueHost {
        override fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
            mFragment.setSurfaceHolderCallback(callback)
        }
    }

    override fun onDetach() {
        Logger.d(this, message = "onDetach")
        mGridViewOverlays = null
        playPauseBtn = null
        playbackOverlaysContainerView = null
        mBackgroundView = null
        mVideoSurface = null
        seekBar = null
        seekBarContainer = null
        contentPositionView = null
        contentDurationView = null
        contentPositionView = null
        videoInfoCodecContainerView = null
        exoPlayerManager.detach(mPlayerListener)
        mGlueHost.setSurfaceHolderCallback(null)
        setSurfaceHolderCallback(null)
        mMediaPlaybackCallback = null
        super.onDetach()
    }

    override fun onStop() {
        timer?.cancel()
        timer = null
        super.onStop()
    }

    override fun getProgressBarManager(): ProgressBarManager {
        return progressManager
    }

    companion object {
        private const val DELAY_AUTO_HIDE_OVERLAY = 5000L
        const val MAX_RETRY_TIME = 3
        private val DEFAULT_OVERLAY_PICK_HEIGHT by lazy {
            200.dpToPx().toFloat()
        }
        private const val SURFACE_NOT_CREATED = 0
        private const val SURFACE_CREATED = 1
    }
}
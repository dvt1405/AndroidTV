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
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.ErrorCode
import com.kt.apps.core.R
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.FocusHighlight
import com.kt.apps.core.base.leanback.HeaderItem
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.base.leanback.ObjectAdapter
import com.kt.apps.core.base.leanback.OnChildLaidOutListener
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.PlaybackControlsRow
import com.kt.apps.core.base.leanback.PlaybackSupportFragment
import com.kt.apps.core.base.leanback.PlaybackSupportFragmentGlueHost
import com.kt.apps.core.base.leanback.PresenterSelector
import com.kt.apps.core.base.leanback.ProgressBarManager
import com.kt.apps.core.base.leanback.SeekBar
import com.kt.apps.core.base.leanback.VerticalGridPresenter
import com.kt.apps.core.base.leanback.media.LeanbackPlayerAdapter
import com.kt.apps.core.base.leanback.media.PlaybackTransportControlGlue
import com.kt.apps.core.base.leanback.media.SurfaceHolderGlueHost
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.base.viewmodels.BaseFavoriteViewModel
import com.kt.apps.core.databinding.LayoutVideoCodecInfoBinding
import com.kt.apps.core.exceptions.MyException
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IVoiceSearchManager
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.inVisible
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.translateY
import com.kt.apps.core.utils.visible
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.Formatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.math.max


private const val MIN_SEEK_DURATION = 30 * 1000

abstract class BasePlaybackFragment : PlaybackSupportFragment(),
    HasAndroidInjector, IMediaKeycodeHandler, ITVGuideKeycodeHandler {
    protected val progressManager by lazy {
        ProgressBarManager()
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var actionLogger: IActionLogger

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    @Inject
    lateinit var voiceSelectorManager: IVoiceSearchManager

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
    private var mBtnProgrammeSchedule: View? = null
    private var playbackOverlaysContainerView: View? = null
    private var mBackgroundView: View? = null
    private var playbackInfoContainerView: LinearLayout? = null
    private var playbackTitleTxtView: TextView? = null
    private var playbackInfoTxtView: TextView? = null
    protected var playBackIsLiveContainer: View? = null
    protected var playbackIsLiveTxtView: TextView? = null
    protected var playbackLiveProgramDuration: TextView? = null
    protected var seekBar: SeekBar? = null
    private var seekBarContainer: ViewGroup? = null
    protected var contentPositionView: TextView? = null
    protected var contentDurationView: TextView? = null
    private var centerContainerView: View? = null
    private var videoInfoCodecContainerView: ViewGroup? = null
    private var btnVideoCodecInfo: ImageView? = null
    private var btnFavouriteVideo: ImageView? = null
    private var btnVoiceSearch: ImageView? = null
    protected var errorDialog: SweetAlertDialog? = null
    protected open var allowDpadUpToOpenSearch = false
    protected var onItemClickedListener: OnItemViewClickedListener? = null
    private val mChildLaidOutListener = OnChildLaidOutListener { _, _, _, _ ->
    }
    private val mGlueHost by lazy {
        BasePlaybackSupportFragmentGlueHost(this@BasePlaybackFragment)
    }
    abstract val numOfRowColumns: Int
    abstract val listProgramForChannelLiveData: LiveData<DataState<List<TVScheduler.Programme>>>

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
                    playPauseBtn?.isActivated = false
                    if (overlaysUIState != OverlayUIState.STATE_HIDDEN
                        && overlaysUIState != OverlayUIState.STATE_ONLY_GRID_CONTENT
                        && !isProgramScheduleShowing()
                        && true != videoInfoCodecContainerView?.isVisible
                    ) {
                        val currentFocus = view?.findFocus()
                        if (currentFocus != null) {
                            handleUI(OverlayUIState.STATE_INIT, true, currentFocus)
                        } else {
                            handleUI(OverlayUIState.STATE_INIT, true)
                        }
                    }
                    exoPlayerManager.exoPlayer?.let {
                        setCodecInfo(it)
                    }
                } else {
                    playPauseBtn?.isActivated = true
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
                        if (!progressManager.isShowing) {
                            progressManager.show()
                        }
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

    protected val formatBuilder = StringBuilder()
    protected val formatter = Formatter(formatBuilder, Locale.getDefault())
    private var contentPosition = 0L

    open fun updateProgress(player: Player?) {
        if (seekBarContainer?.isVisible == false) {
            return
        }
        player?.let {
            val realDurationMillis: Long = player.duration
            seekBar?.max = realDurationMillis.toInt()
            contentPosition = player.contentPosition
            seekBar?.setSecondaryProgress(player.bufferedPosition.toInt())
            seekBar?.progress = player.contentPosition.toInt()
            val contentPosition =
                "${Util.getStringForTime(formatBuilder, formatter, player.contentPosition)} /"
            val contentDuration =
                " ${Util.getStringForTime(formatBuilder, formatter, player.contentDuration)}"
            contentPositionView?.text = contentPosition
            contentDurationView?.text = contentDuration
        }
    }

    private fun setCodecInfo(player: ExoPlayer) {
        val currentProgram = getCurrentProgram()
        if (currentProgram != null) {
            view?.findViewById<TextView>(R.id.video_title)?.text = currentProgram.title.takeIf {
                it.isNotEmpty()
            } ?: player.mediaMetadata.title
            view?.findViewById<TextView>(R.id.video_duration)?.text =
                if (currentProgram.start.isNotEmpty() && currentProgram.stop.isNotEmpty()) {
                    Util.getStringForTime(
                        formatBuilder,
                        formatter,
                        currentProgram.endTimeMilli() - currentProgram.startTimeMilli()
                    )
                } else {
                    "LIVE"
                }
        } else {
            if (player.contentDuration < 120_000) {
                view?.findViewById<TextView>(R.id.video_duration)?.text = "LIVE"
                view?.findViewById<TextView>(R.id.video_duration_title)?.visible()
            } else {
                view?.findViewById<TextView>(R.id.video_duration_title)?.visible()
                view?.findViewById<TextView>(R.id.video_duration)
                    ?.let {
                        it.visible()
                        it.text = Util.getStringForTime(
                            formatBuilder,
                            formatter,
                            player.contentDuration
                        )
                    }
            }
        }
        player.videoSize.width.takeIf { it > 0 }
            ?.let {
                "${player.videoSize.width}x${player.videoSize.height}"
            }?.let {
                view?.findViewById<TextView>(R.id.video_resolution)?.text = it
            }
        player.videoFormat?.codecs?.let {
            view?.findViewById<TextView>(R.id.video_codec)?.text = it
        }
        view?.findViewById<TextView>(R.id.video_frame_rate)?.text =
            "${player.videoFormat?.frameRate?.takeIf { 
                it > 0.0
            } ?: "No Value"}"
        view?.findViewById<TextView>(R.id.byte_rate)?.text =
            "${player.videoDecoderCounters?.renderedOutputBufferCount}"
        view?.findViewById<TextView>(R.id.audio_codec)?.text = player.audioFormat?.codecs.takeIf {
            !it?.trim().isNullOrEmpty()
        } ?: "NoValue"
    }

    open fun getCurrentProgram(): TVScheduler.Programme? {
        return null
    }

    open fun onPlayerPlaybackStateChanged(playbackState: Int) {

    }
    open fun onHandlePlayerError(error: PlaybackException) {
        Logger.e(this, tag = "onHandlePlayerError", exception = error)
    }

    private val mHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            private var _cancelAtTime: Long = SystemClock.uptimeMillis()
            private val token = Any()
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Logger.d(
                    this@BasePlaybackFragment, "HandleMessage", "{" +
                            "what: ${msg.what}, " +
                            "when: ${msg.`when`}," +
                            "_cancelAtTime: $_cancelAtTime}"
                )
                if (msg.`when` < _cancelAtTime) {
                    Logger.d(this@BasePlaybackFragment, "HandleMessage", "Cancel")
                    return
                }
                when (msg.what) {
                    MSG_CANCEL_AUTO_HIDE -> {
                        _cancelAtTime = msg.`when`
                    }

                    MSG_FORCE_HIDE_OVERLAY -> {
                        removeCallbacksAndMessages(null)
                        autoHideOverlayRunnable.run()
                        _cancelAtTime = msg.`when`
                    }

                    MSG_AUTO_HIDE_OVERLAY -> {
                        if (msg.`when` - DELAY_AUTO_HIDE_OVERLAY >= _cancelAtTime) {
                            removeCallbacksAndMessages(null)
                            autoHideOverlayRunnable.run()
                            _cancelAtTime = msg.`when`
                        } else {
                            Logger.d(
                                this@BasePlaybackFragment,
                                "HandleMessage",
                                "MSG_HIDE_OVERLAY Cancel"
                            )
                        }
                    }

                    MSG_INCREASE_AUTO_HIDE_TIMEOUT -> {
                        removeCallbacksAndMessages(null)
                        sendMessageAtTime(
                            obtainMessage(msg.arg1, msg.obj),
                            SystemClock.uptimeMillis() + DELAY_AUTO_HIDE_OVERLAY
                        )
                    }
                }
            }
        }
    }
    private val autoHideOverlayRunnable by lazy {
        Runnable {
            if (isProgramScheduleShowing()) {
                return@Runnable
            }
            playPauseBtn?.visible()
            playPauseBtn?.requestFocus()
            playbackOverlaysContainerView?.fadeOut(true) {
                Logger.d(this@BasePlaybackFragment, "autoHideOverlayRunnable", "fadeOut")
                playPauseBtn?.alpha = 0f
                mGridViewHolder?.gridView?.selectedPosition = 0
                mGridViewOverlays?.translationY = mGridViewPickHeight
                mSelectedPosition = 0
                if (!isProgramScheduleShowing()) {
                    mGridViewHolder?.gridView?.clearFocus()
                }
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
        val videoCodecInfoBinding = DataBindingUtil.bind<LayoutVideoCodecInfoBinding>(videoInfoCodecContainerView!!)
        root.addView(videoInfoCodecContainerView)
        mBackgroundView = root.findViewById(androidx.leanback.R.id.playback_fragment_background)
        playbackTitleTxtView = root.findViewById(R.id.playback_title)
        playbackInfoTxtView = root.findViewById(R.id.playback_info)
        playBackIsLiveContainer = root.findViewById(R.id.playback_live_container)
        playbackIsLiveTxtView = root.findViewById(R.id.playback_live)
        playbackLiveProgramDuration = root.findViewById(R.id.playback_live_program_duration)
        playbackInfoContainerView = root.findViewById(R.id.info_container)
        playPauseBtn = root.findViewById(R.id.ic_play_pause)
        seekBar = root.findViewById(R.id.video_progress_bar)
        seekBarContainer = root.findViewById(R.id.progress_bar_container)
        contentPositionView = root.findViewById(R.id.content_position)
        contentDurationView = root.findViewById(R.id.content_duration)
        btnVideoCodecInfo = root.findViewById(R.id.btn_video_codec_info)
        centerContainerView = root.findViewById(R.id.center_controls_container)
        btnFavouriteVideo = root.findViewById(R.id.btn_favourite)
        mBtnProgrammeSchedule = root.findViewById(R.id.btn_program_list)
        btnVoiceSearch = root.findViewById(R.id.btn_voice)
        progressManager.setRootView(centerContainerView!! as ViewGroup)
        hideControlsOverlay(false)
        val controlBackground = root.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground.makeGone()
        playPauseBtn?.setOnClickListener {
            onPlayPauseIconClicked()
        }
        progressManager.setOnProgressShowHideListener { show ->
            if (isProgramScheduleShowing()) {
                return@setOnProgressShowHideListener
            }
            val currentFocus = view?.findFocus()
            if (show) {
                if (overlaysUIState == OverlayUIState.STATE_INIT
                    || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                ) {
                    if (currentFocus != null) {
                        handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true, currentFocus)
                    } else {
                        handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
                    }
                }
            } else {
                if (overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
                    || overlaysUIState == OverlayUIState.STATE_INIT
                ) {
                    btnFavouriteVideo?.fadeIn {}
                    playPauseBtn?.fadeIn {
                        if (currentFocus == null) {
                            focusedPlayBtnIfNotSeeking()
                        }
                        overlaysUIState = OverlayUIState.STATE_INIT
                    }
                }
                if (true == exoPlayerManager.exoPlayer?.isPlaying) {
                    increaseAutoHideTimeout()
                }
            }
        }
        videoInfoCodecContainerView?.gone()
        btnVideoCodecInfo?.setOnClickListener {
            Message.obtain(mHandler, MSG_FORCE_HIDE_OVERLAY).sendToTarget()
            autoHideOverlayRunnable.run()
            videoInfoCodecContainerView?.fadeIn {}
        }
        btnFavouriteVideo?.setOnClickListener {
            it.isSelected = !it.isSelected
            onFavoriteVideoClicked(it.isSelected)
            increaseAutoHideTimeout()
        }
        mBtnProgrammeSchedule?.setOnClickListener {
            handleUiShowProgramSchedule()
        }
        btnVoiceSearch?.setOnClickListener {
            it.isSelected = !it.isSelected
            startVoiceSearch()
            increaseAutoHideTimeout()
        }
        btnVoiceSearch?.setOnLongClickListener {
            it.isSelected = !it.isSelected
            startVoiceSearch(bundleOf(
                IVoiceSearchManager.EXTRA_RESET_SETTING to true
            ))
            increaseAutoHideTimeout()
            return@setOnLongClickListener false
        }
        playbackInfoTxtView?.setOnClickListener {
            val maxLines = playbackInfoTxtView?.maxLines ?: 0
            if (maxLines < 100) {
                playbackInfoTxtView?.maxLines = 100
            } else {
                playbackInfoTxtView?.maxLines = 2
            }
        }
        return root
    }

    private fun handleUiShowProgramSchedule() {
        playbackOverlaysContainerView?.fadeOut {}
        overlaysUIState = OverlayUIState.STATE_HIDDEN
        onShowProgramSchedule()
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
        favoriteViewModel?.saveIptvChannelLiveData?.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                Toast.makeText(
                    requireContext(),
                    "Đã thêm vào danh sách yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (it is DataState.Error) {
                Toast.makeText(
                    requireContext(),
                    "Thêm vào danh sách yêu thích thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        favoriteViewModel?.deleteIptvChannelLiveData?.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                Toast.makeText(
                    requireContext(),
                    "Đã xóa khỏi danh sách yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (it is DataState.Error) {
                Toast.makeText(
                    requireContext(),
                    "Xóa khỏi danh sách yêu thích thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        listProgramForChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    if (it.data.isNotEmpty() && it.data.size > 1) {
                        mBtnProgrammeSchedule?.visible()
                    } else {
                        mBtnProgrammeSchedule?.gone()
                    }
                }

                is DataState.Error -> {
                    val error = it.throwable
                    if (error is MyException) {
                        if (error.code == ErrorCode.UN_SUPPORT_SHOW_PROGRAM) {
                            mBtnProgrammeSchedule?.gone()
                        }
                    }
                }

                else -> {

                }
            }
        }
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
        try {
            if (isProgramScheduleShowing() || true == videoInfoCodecContainerView?.isVisible) {
                return
            }
            if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
                handleUI(OverlayUIState.STATE_INIT, true)
            }
            if (true == exoPlayerManager.playerAdapter?.isPlaying) {
                exoPlayerManager.playerAdapter?.pause()
                Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
            } else {
                exoPlayerManager.playerAdapter?.play()
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
        mGridViewHolder?.view?.nextFocusUpId = R.id.ic_play_pause
        mGridViewOverlays?.nextFocusUpId = R.id.ic_play_pause
    }

    open fun onCreateProgramScheduleFragment(): Fragment? {
        return null
    }

    open fun onShowProgramSchedule() {
        val programFragment = onCreateProgramScheduleFragment()
        if (!isAdded || programFragment == null) {
            return
        }
        val rootView = (view as? ViewGroup ?: return)
        if (rootView.findViewById<View?>(R.id.container_program) == null) {
            val frameProgram = LayoutInflater.from(requireContext())
                .inflate(R.layout.container_program, rootView, false)

            rootView.addView(frameProgram)
        } else {
            view?.findViewById<View?>(R.id.container_program)
                ?.fadeIn{}
        }

        fun commitNewFragment() {
            childFragmentManager.beginTransaction()
                .replace(R.id.container_program, programFragment)
                .commitNow()
        }
        childFragmentManager.findFragmentById(R.id.container_program)?.let {
            if (it.isHidden || it.isDetached || it.isRemoving) {
                commitNewFragment()
            }
        } ?: commitNewFragment()
    }
    open fun onHideProgramSchedule() {
        handleUI(OverlayUIState.STATE_HIDDEN, true)
        mBtnProgrammeSchedule?.clearFocus()
        playPauseBtn?.requestFocus()
        if (isAdded) {
            view?.findViewById<View?>(R.id.container_program)
                ?.fadeOut()
            childFragmentManager.findFragmentById(R.id.container_program)?.let {
                if (it.isVisible) {
                    childFragmentManager.beginTransaction()
                        .remove(it)
                        .commitNow()
                }
            }
        }
    }
    open fun isProgramScheduleShowing(): Boolean {
        return try {
            isAdded &&
                    childFragmentManager.findFragmentById(R.id.container_program)?.isAdded == true &&
                    childFragmentManager.findFragmentById(R.id.container_program)?.isDetached != true &&
                    childFragmentManager.findFragmentById(R.id.container_program)?.isVisible == true
        } catch (e: Exception) {
            Logger.e(this, exception = e)
            false
        }
    }

    fun prepare(
        title: String,
        subTitle: String?,
        isLive: Boolean,
        showProgressManager: Boolean = true
    ) {
        Logger.d(this@BasePlaybackFragment, "ShowVideoInfo", "$title - $subTitle - $isLive")
        Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
        setVideoInfo(title, subTitle, isLive)

        if (showProgressManager) {
            progressManager.show()
        }
        if (progressManager.isShowing) {
            showAllOverlayElements(false)
        } else {
            showAllOverlayElements(true)
        }
        seekBarContainer?.visible()
    }

    private fun increaseAutoHideTimeout() {
        Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
        sendMessageAutoHideAfterDelayedTime()
    }

    private fun sendMessageAutoHideAfterDelayedTime() {
        val message = Message.obtain(mHandler, MSG_INCREASE_AUTO_HIDE_TIMEOUT)
        message.obj = autoHideOverlayRunnable
        message.arg1 = MSG_AUTO_HIDE_OVERLAY
        message.sendToTarget()
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
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        ensureUIByState(overlaysUIState)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        onAnimationEnd(animation)
                    }

                    fun ensureUIByState(state: OverlayUIState) {
                        when (state) {
                            OverlayUIState.STATE_INIT -> {
                                mGridViewHolder?.gridView?.clearFocus()
//                                focusedPlayBtnIfNotSeeking()
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
    private fun handleUI(targetState: OverlayUIState,
                         autoHide: Boolean,
                         nextFocusRequest: View? = null) {
        Logger.d(
            this@BasePlaybackFragment,
            tag = "HandleUI",
            message = "currentState: $overlaysUIState," +
                    "targetState: $targetState"
        )
        val currentState = overlaysUIState
        when (targetState) {
            OverlayUIState.STATE_INIT -> {
                showOverlayPlaybackControl(currentState, nextFocusRequest)
            }

            OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY -> {
                showOverlayPlaybackControlWithoutBtnPlay(currentState, nextFocusRequest)
            }

            OverlayUIState.STATE_ONLY_GRID_CONTENT -> {
                Logger.d(this, message = "y = ${mGridViewOverlays?.translationY} - $mGridViewPickHeight")
                fun onlyShowGridView() {
                    playbackInfoContainerView?.fadeOut()
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
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            fadeInAnimator.setupEndValues()
                            overlayRootContainerAnimationUpdateListener.onAnimationUpdate(fadeInAnimator)
                            onlyShowGridView()
                            fadeInAnimator.removeListener(this)
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            super.onAnimationCancel(animation)
                            onAnimationEnd(animation)
                        }
                    })
                    fadeInAnimator.resume()
                } else if (playbackOverlaysContainerView?.visibility != View.VISIBLE) {
                    fadeInAnimator.removeAllUpdateListeners()
                    fadeInAnimator.addUpdateListener(overlayRootContainerAnimationUpdateListener)
                    fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            onlyShowGridView()
                            overlayRootContainerAnimationUpdateListener.onAnimationUpdate(ValueAnimator.ofFloat(1f))
                            fadeInAnimator.removeListener(this)
                        }

                        override fun onAnimationCancel(animation: Animator) {
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
                Message.obtain(mHandler, MSG_FORCE_HIDE_OVERLAY).sendToTarget()
            }
        }

        Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
        if (autoHide) {
            sendMessageAutoHideAfterDelayedTime()
        }
        overlaysUIState = targetState
    }

    private fun showOverlayPlaybackControl(currentState: OverlayUIState,
                                           nextFocusRequest: View? = null) {
        if (currentState == OverlayUIState.STATE_ONLY_GRID_CONTENT) {
            mGridViewHolder?.gridView?.scrollToPosition(0)
            focusedPlayBtnIfNotSeeking(nextFocusRequest)
            fadeInAnimator.cancel()
            fadeInAnimator.removeAllUpdateListeners()
            fadeInAnimator.addUpdateListener(btnPlayPauseAnimationUpdateListener)
            fadeInAnimator.addUpdateListener(playbackInfoAnimationUpdateListener)
            fadeInAnimator.start()
            mGridViewOverlays?.translateY(mGridViewPickHeight, onAnimationEnd = {
                focusWhenInitOverlay(nextFocusRequest)
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
                    focusedPlayBtnIfNotSeeking(nextFocusRequest)
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
            focusedPlayBtnIfNotSeeking(nextFocusRequest)
        }
    }

    private fun showOverlayPlaybackControlWithoutBtnPlay(currentState: OverlayUIState,
                                                         nextFocusRequest: View? = null) {
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
        focusedPlayBtnIfNotSeeking(nextFocusRequest)
    }

    private fun focusedPlayBtnIfNotSeeking(nextFocusRequest: View? = null) {
        nextFocusRequest?.requestFocus() ?: kotlin.run {
            if (true != playPauseBtn?.isFocused) {
                playPauseBtn?.requestFocus()
            }
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
        errorDialog?.dismissWithAnimation()
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
        increaseAutoHideTimeout()
        when {
            forceShowVideoInfoContainer && !isProgramScheduleShowing() -> {
                setVideoInfo(
                    playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_TITLE],
                    playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION],
                    isLive
                )
                showAllOverlayElements(false)
            }

            !forceShowVideoInfoContainer && overlaysUIState == OverlayUIState.STATE_ONLY_GRID_CONTENT -> {
                showAllOverlayElements(true)
            }
        }
        seekBarContainer?.visible()
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

    protected fun updateVideoInfo(title: String?, description: String?, isLive: Boolean = false) {
        if (!playbackTitleTxtView?.text.toString().equals(title, ignoreCase = true)) {
            playbackTitleTxtView?.text = title
        }
        if (!playbackInfoTxtView?.text.toString().equals(description, ignoreCase = true)) {
            playbackInfoTxtView?.text = description?.trim()
        }
        if (playbackTitleTxtView?.isSelected != true) {
            playbackTitleTxtView?.isSelected = true
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
            playBackIsLiveContainer?.visible()
        } else {
            playBackIsLiveContainer?.gone()
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
        if (!isAdded) {
            return true
        }
        return overlaysUIState == OverlayUIState.STATE_HIDDEN && videoInfoCodecContainerView?.isVisible != true
                && childFragmentManager.findFragmentById(R.id.container_program).let {
            it == null || !it.isVisible
        }
    }

    fun hideOverlay() {
        if (!isAdded) {
            return
        }
        if (isProgramScheduleShowing()) {
            onHideProgramSchedule()
        } else if (videoInfoCodecContainerView?.isVisible == true) {
            videoInfoCodecContainerView?.fadeOut {
                handleUI(OverlayUIState.STATE_HIDDEN, true)
                mBtnProgrammeSchedule?.clearFocus()
                playPauseBtn?.requestFocus()
                videoInfoCodecContainerView?.gone()
            }
            sendMessageAutoHideAfterDelayedTime()
        } else {
            videoInfoCodecContainerView?.gone()
            Message.obtain(mHandler, MSG_FORCE_HIDE_OVERLAY).sendToTarget()
        }
    }

    override fun onDestroyView() {
        mVideoSurface = null
        mState = SURFACE_NOT_CREATED
        super.onDestroyView()
    }

    override fun onDpadCenter() {
        if (isProgramScheduleShowing() || true == videoInfoCodecContainerView?.isVisible) {
            return
        }
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            if (isPlaying()) {
                if (progressManager.isShowing) {
                    handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
                } else {
                    handleUI(OverlayUIState.STATE_INIT, true)
                }
            } else {
                if (progressManager.isShowing) {
                    handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, false)
                } else {
                    handleUI(OverlayUIState.STATE_INIT, false)
                }
            }
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
        if (true == videoInfoCodecContainerView?.isVisible || isProgramScheduleShowing()) {
            return
        }
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            if (progressManager.isShowing) {
                handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
            } else {
                handleUI(OverlayUIState.STATE_INIT, true)
            }
        } else if (overlaysUIState == OverlayUIState.STATE_INIT
            || overlaysUIState == OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY
        ) {
            if (playbackInfoTxtView?.isFocused == true) {
//                seekBar?.requestFocus()
            } else if ((seekBar?.isFocused == true)
                && (seekBarContainer?.visibility == View.VISIBLE)
            ) {
                playPauseBtn?.requestFocus()
                increaseAutoHideTimeout()
            } else {
                handleUI(OverlayUIState.STATE_ONLY_GRID_CONTENT, true)
            }
        } else {
            increaseAutoHideTimeout()
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
            true == videoInfoCodecContainerView?.isVisible || isProgramScheduleShowing() -> {

            }
            !isMenuShowed() -> {
                Logger.d(this, message = "showGridMenu")
                showGridMenu()
            }

            seekBarContainer?.visibility == View.VISIBLE && seekBar?.isFocused == true -> {
                Logger.d(this, message = "seekBarContainer?.visibility == View.VISIBLE && seekBar?.isFocused == true")
                if (overlaysUIState == OverlayUIState.STATE_INIT
                    && playPauseBtn!!.alpha > 0f
                ) {
                    increaseAutoHideTimeout()
                    if (allowDpadUpToOpenSearch) {
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
            }

            playPauseBtn?.isFocused == true -> {
                Logger.d(this, message = "playPauseBtn?.isFocused == true")
                increaseAutoHideTimeout()
            }

            mGridViewHolder!!.gridView.selectedPosition in 0..4 -> {
                Logger.d(this, message = "mGridViewHolder!!.gridView.selectedPosition: ${mGridViewHolder!!.gridView.selectedPosition}")
                if (progressManager.isShowing) {
                    handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true)
                } else {
                    handleUI(OverlayUIState.STATE_INIT, true)
                }
            }
        }
    }

    open fun startVoiceSearch(extraData: Bundle? = null) {
        CompositeDisposable().add(
            extraData?.let {
                voiceSelectorManager.openVoiceAssistant(it).subscribe {
                }
            } ?: voiceSelectorManager.openVoiceAssistant().subscribe {
            }
        )
    }

    private fun focusWhenInitOverlay(nextFocusRequest: View?) {
        nextFocusRequest?.requestFocus() ?: playPauseBtn?.requestFocus()
    }

    override fun onDpadLeft() {
        if (true == videoInfoCodecContainerView?.isVisible || isProgramScheduleShowing()) {
            return
        }
        if (seekBar?.isFocused == true && seekBar?.isSeekAble == true) {
            exoPlayerManager.exoPlayer?.let {
                if (contentPosition - MIN_SEEK_DURATION >= 0) {
                    it.seekTo(contentPosition - MIN_SEEK_DURATION)
                } else if (contentPosition >= 0) {
                    it.seekTo(0L)
                }
                updateProgress(it)
            }
        }
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            if (progressManager.isShowing) {
                handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true, btnVoiceSearch)
            } else {
                handleUI(OverlayUIState.STATE_INIT, true, btnVoiceSearch)
            }
        } else {
            increaseAutoHideTimeout()
        }
    }

    override fun onKeyCodeForward() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition + MIN_SEEK_DURATION)
    }

    override fun onKeyCodeRewind() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition - MIN_SEEK_DURATION)
    }

    override fun onDpadRight() {
        if (true == videoInfoCodecContainerView?.isVisible || isProgramScheduleShowing()) {
            return
        }
        if (seekBar?.isFocused == true && seekBar?.isSeekAble == true) {
            exoPlayerManager.exoPlayer?.let {
                if (contentPosition + MIN_SEEK_DURATION <= it.duration) {
                    it.seekTo(contentPosition + MIN_SEEK_DURATION)
                } else if (contentPosition < it.duration) {
                    it.seekTo(it.duration - contentPosition)
                }
                updateProgress(it)
            }
        }
        if (overlaysUIState == OverlayUIState.STATE_HIDDEN) {
            if (progressManager.isShowing) {
                handleUI(OverlayUIState.STATE_INIT_WITHOUT_BTN_PLAY, true, btnFavouriteVideo)
            } else {
                handleUI(OverlayUIState.STATE_INIT, true, btnFavouriteVideo)
            }
        } else {
            increaseAutoHideTimeout()
        }
    }

    override fun onKeyCodeChannelUp() {
        increaseAutoHideTimeout()
    }

    override fun onKeyCodeChannelDown() {
        increaseAutoHideTimeout()
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
        increaseAutoHideTimeout()
    }

    override fun onKeyCodePlay() {
        increaseAutoHideTimeout()
    }

    override fun onKeyCodeMenu() {
        handleUI(OverlayUIState.STATE_ONLY_GRID_CONTENT, true)

    }

    fun showErrorDialogWithErrorCode(errorCode: Int, errorMessage: String? = null, onDismiss: () -> Unit = {}) {
        if (this.isDetached || this.isHidden || !this.isAdded || context == null
            || this.isRemoving) {
            return
        }
        playPauseBtn?.isActivated = true
        errorDialog = showErrorDialog(
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
                    Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
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
                    Message.obtain(mHandler, MSG_CANCEL_AUTO_HIDE).sendToTarget()
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
    fun isPlaying(): Boolean {
        return exoPlayerManager.exoPlayer?.isPlaying == true
    }

    open fun onRefreshProgram() {

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
        mBtnProgrammeSchedule = null
        super.onDetach()
    }

    override fun onKeyCodeProgram() {
        handleUiShowProgramSchedule()
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
        private const val MSG_CANCEL_AUTO_HIDE = 1000
        private const val MSG_AUTO_HIDE_OVERLAY = 1001
        private const val MSG_INCREASE_AUTO_HIDE_TIMEOUT = 1002
        private const val MSG_FORCE_HIDE_OVERLAY = 1003
        private const val DELAY_AUTO_HIDE_OVERLAY = 7_000L
        const val MAX_RETRY_TIME = 3
        private val DEFAULT_OVERLAY_PICK_HEIGHT by lazy {
            200.dpToPx().toFloat()
        }
        private const val SURFACE_NOT_CREATED = 0
        private const val SURFACE_CREATED = 1
    }
}
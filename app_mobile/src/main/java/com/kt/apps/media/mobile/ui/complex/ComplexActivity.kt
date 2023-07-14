package com.kt.apps.media.mobile.ui.complex

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.*
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.logPlaybackShowError
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.NoNetworkException
import com.kt.apps.media.mobile.models.PlaybackFailException
import com.kt.apps.media.mobile.ui.fragments.channels.IPlaybackAction
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.WrappedTVChannelViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.lang.ref.WeakReference
import java.util.concurrent.TimeoutException
import javax.inject.Inject

enum class  PlaybackState {
    Fullscreen, Minimal, Invisible
}
class ComplexActivity : BaseActivity<ActivityComplexBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var logger: IActionLogger

    override val layoutRes: Int
        get() = R.layout.activity_complex

    private var layoutHandler: ComplexLayoutHandler? = null

    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(this, factory)[WrappedTVChannelViewModel::class.java].apply {
            this.tvWithLinkStreamLiveData.observe(this@ComplexActivity, linkStreamDataObserver)
        }
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
    }

    private val networkStateViewModel: NetworkStateViewModel? by lazy {
        ViewModelProvider(this, factory)[NetworkStateViewModel::class.java]
    }

    private val linkStreamDataObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {dataState ->
            when(dataState) {
                is DataState.Loading ->
                    layoutHandler?.onStartLoading()
                is DataState.Error -> handleError(dataState.throwable)
                else -> return@Observer
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        val metrics = resources.displayMetrics
        layoutHandler = if (metrics.widthPixels <= metrics.heightPixels) {
            PortraitLayoutHandler(WeakReference(this))
        } else {
            LandscapeLayoutHandler(WeakReference(this))
        }

        layoutHandler?.onPlaybackStateChange = {
            playbackViewModel.changeDisplayState(it)
        }

    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel?.apply {
            tvWithLinkStreamLiveData.observe(this@ComplexActivity, linkStreamDataObserver)
            tvChannelLiveData.observe(this@ComplexActivity) {dataState ->
                when(dataState) {
                    is DataState.Error -> handleError(dataState.throwable)
                    else -> { }
                }
            }
        }

        binding.fragmentContainerPlayback.getFragment<PlaybackFragment>().apply {
            this.callback = object: IPlaybackAction {
                override fun onLoadedSuccess(videoSize: VideoSize) {
                    layoutHandler?.onLoadedVideoSuccess(videoSize)
                }

                override fun onOpenFullScreen() {
                    layoutHandler?.onOpenFullScreen()
                }

                override fun onPauseAction(userAction: Boolean) {
                    if (userAction) layoutHandler?.onPlayPause(isPause = true)
                }

                override fun onPlayAction(userAction: Boolean) {
                    if (userAction) layoutHandler?.onPlayPause(isPause = false)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    networkStateViewModel?.networkStatus?.
                    collectLatest {state ->
                        if (state == NetworkState.Unavailable)
                            showNoNetworkAlert(autoHide = true)
                    }
                }

                launch {
                    playbackViewModel.state.collectLatest { state ->
                        when (state) {
                            is PlaybackViewModel.State.FINISHED ->
                                if (state.error != null && state.error is PlaybackFailException) {
                                    logger.logPlaybackShowError(
                                        state.error.error,
                                        tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: ""
                                    )
                                    handleError(state.error)
                                }
                            else -> { }
                        }
                    }
                }
            }
        }

        //Deeplink handle
        handleIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.fragmentContainerPlayback.getFragment<PlaybackFragment>().onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onBackPressed() {
        if (layoutHandler?.onBackEvent() == true) {
            return
        }
        super.onBackPressed()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        layoutHandler?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val deeplink = intent?.data ?: return

        if (deeplink.host?.equals(Constants.HOST_TV) == true || deeplink.host?.equals(Constants.HOST_RADIO) == true) {
            if(deeplink.path?.contains("channel") == true) {
                tvChannelViewModel.playMobileTvByDeepLinks(uri = deeplink)
                intent.data = null
            } else {
                intent.data = null
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        when (throwable) {
            is NoNetworkException -> showNoNetworkAlert()
            is TimeoutException -> showErrorAlert("Đã xảy ra lỗi, hãy kiểm tra kết nối mạng")
            is PlaybackFailException -> {
                val error = throwable.error
                val channelName = (tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "")
                val message = "Kênh $channelName hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí: ${error.errorCode} ${error.message}"
                showErrorAlert(message)
            }
            else -> {
                showErrorAlert("Lỗi")
            }
        }
    }
    private fun showNoNetworkAlert(autoHide: Boolean = false) {
        val dialog = AlertDialog.Builder(this, R.style.WrapContentDialog).apply {
            setCancelable(true)
            setView(layoutInflater.inflate(R.layout.no_internet_dialog, null))
        }
            .create().apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
            }
        dialog.show()
        if (autoHide) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1200)
                dialog.dismiss()
            }
        }
    }

    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this, R.style.WrapContentDialog).apply {
            setCancelable(true)
            setView(layoutInflater.inflate(R.layout.error_dialog, null).apply {
                findViewById<MaterialTextView>(R.id.alert_message).text = message
            })
        }
            .create()
            .show()


    }

}
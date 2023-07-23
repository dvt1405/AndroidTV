package com.kt.apps.media.mobile.ui.complex

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.*
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.logPlaybackShowError
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.*
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.models.NoNetworkException
import com.kt.apps.media.mobile.models.PlaybackFailException
import com.kt.apps.media.mobile.ui.fragments.channels.IPlaybackAction
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.viewmodels.ComplexViewModel
import com.kt.apps.media.mobile.viewmodels.StreamLinkData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val playbackFragment: PlaybackFragment by lazy {
        binding.fragmentContainerPlayback.getFragment()
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
    }

    private val networkStateViewModel: NetworkStateViewModel? by lazy {
        ViewModelProvider(this, factory)[NetworkStateViewModel::class.java]
    }

    private val viewModel: ComplexViewModel by lazy {
        ComplexViewModel(ViewModelProvider(this, factory), lifecycleScope)
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
        binding.parseSourceLoadingContainer?.visibility = View.INVISIBLE

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutHandler?.onBackEvent() == true) {
                    return
                }
                finish()
            }
        })
     }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initAction(savedInstanceState: Bundle?) {


        playbackFragment.apply {
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

                override fun onExitMinimal() {
                    layoutHandler?.onCloseMinimal()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.networkStatus.collectLatest {state ->
                        if (state == NetworkState.Unavailable)
                            showNoNetworkAlert(autoHide = true)
                    }
                }
            }
        }

        val addSourceState = MutableStateFlow<AddSourceState>(AddSourceState.IDLE)
        viewModel.addSourceState.onEach { addSourceState.value = it }.launchIn(lifecycleScope)

        lifecycleScope.launchWhenStarted {
            addSourceState
                .collectLatest {
                    when(it) {
                        is AddSourceState.StartLoad -> {
                            binding.parseSourceLoadingContainer?.fadeIn {
                                binding.parseSourceLoadingContainer?.invalidate()
                            }
                            binding.statusView?.startLoading()
                            binding.loadingDescription?.text = "Đang thêm nguồn: ${it.source.sourceUrl}..."
                        }
                        is AddSourceState.Success -> {
                            binding.statusView?.showSuccess()
                            binding.loadingDescription?.text = "Đã thêm nguồn: ${it.source.sourceUrl}"
                        }
                        is AddSourceState.Error -> {
                            binding.statusView?.showError()
                            binding.loadingDescription?.text = "Xảy ra lỗi"
                        }
                        else -> {
                            binding.parseSourceLoadingContainer?.fadeOut {  }
                            binding.loadingDescription?.text = ""
                        }
                    }
                    if (it is AddSourceState.Success || it is AddSourceState.Error) {
                        delay(1500)
                        binding.parseSourceLoadingContainer?.fadeOut {  }
                        binding.loadingDescription?.text = ""
                    }
                }
        }



        lifecycleScope.launchWhenStarted {
            addSourceState
                .filter { it is AddSourceState.Success }
                .collectLatest {
                    delay(500)
                    onAddedExtension()
                }
        }

        lifecycleScope.launchWhenStarted {
            addSourceState
                .filter { it is AddSourceState.Error }
                .collectLatest {
                    delay(500)
                    showErrorAlert("Đã xảy ra lỗi vui lòng thử lại sau")
                }
        }


        lifecycleScope.launchWhenStarted {
            viewModel.playbackProcessState
                .collectIndexed { index, value ->
                    Log.d(TAG, "Ann playbackProcessState: $index $value ")
                }
        }

        val playbackState = viewModel.playbackProcessState

        playbackState
            .onEach { Log.d(TAG, "Ann playbackState: $it") }
            .launchIn(lifecycleScope)

        playbackState.filter { it is PlaybackViewModel.State.LOADING || it is PlaybackViewModel.State.PLAYING }
            .onEach { layoutHandler?.onStartLoading() }
            .launchIn(lifecycleScope)

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
//                tvChannelViewModel.playMobileTvByDeepLinks(uri = deeplink)
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
//                val channelName = (tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "")
                val message = "Kênh hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí: ${error.errorCode} ${error.message}"
                showErrorAlert(message)
            }
            else -> {
                showErrorAlert("Lỗi")
            }
        }
    }

    private fun onAddedExtension() {
        Log.d(TAG, "onAddedExtension: success")
//        showSuccessDialog()
        showSuccessDialog(
            content = "Thêm nguồn kênh thành công!\r\nKhởi động lại ứng dụng để kiểm tra nguồn kênh"
        )
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
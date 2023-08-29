package com.kt.apps.media.mobile.ui.complex

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.models.*
import com.kt.apps.media.mobile.ui.fragments.models.AddSourceState
import com.kt.apps.media.mobile.ui.fragments.playback.BasePlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.playback.FootballPlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.playback.IDispatchTouchListener
import com.kt.apps.media.mobile.ui.fragments.playback.IPTVPlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.playback.IPlaybackAction
import com.kt.apps.media.mobile.ui.fragments.playback.RadioPlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.playback.TVPlaybackFragment
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.viewmodels.ComplexInteractors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.TimeoutException
import javax.inject.Inject


class ComplexActivity : BaseActivity<ActivityComplexBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var logger: IActionLogger

    override val layoutRes: Int
        get() = R.layout.activity_complex

    private var layoutHandler: ComplexLayoutHandler? = null

    private var touchListenerList: MutableList<IDispatchTouchListener> = mutableListOf()

    private val viewModel: ComplexInteractors by lazy {
        ComplexInteractors(ViewModelProvider(this, factory), lifecycleScope)
    }

    private val playbackAction = object: IPlaybackAction {
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

    override fun initView(savedInstanceState: Bundle?) {
        val metrics = resources.displayMetrics
        layoutHandler = if (metrics.widthPixels <= metrics.heightPixels) {
            PortraitLayoutHandler(WeakReference(this))
        } else {
            LandscapeLayoutHandler(WeakReference(this))
        }

        layoutHandler?.onPlaybackStateChange = {
            lifecycleScope.launch {
                viewModel.onChangePlayerState(it)
            }
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

        binding.fragmentContainerPlayback.getFragment<BasePlaybackFragment<*>>()?.apply {
            this.callback = playbackAction
        }
     }

    override fun initAction(savedInstanceState: Bundle?) {
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                viewModel.networkStatus.collectLatest {state ->
                    if (state == NetworkState.Unavailable)
                        showNoNetworkAlert(autoHide = true)
                }
            }
        }
        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.openPlaybackEvent.collectLatest {
                    loadPlayback(it)
                }
            }

            launch {
                viewModel.addSourceState.filter { it is AddSourceState.Success }
                    .collectLatest {
                        delay(500)
                        onAddedExtension()
                    }
            }

            launch {
                viewModel.addSourceState.filter { it is AddSourceState.Error }
                    .collectLatest {
                        delay(500)
                        showErrorDialog(titleText = "Đã xảy ra lỗi vui lòng thử lại sau")
                    }
            }

            launch {
                viewModel.addSourceState.collectLatest {
                    handleAddSourceState(it)
                }
            }

            launch {
                viewModel.playerState.collectLatest { state ->
                    layoutHandler?.confirmState(state)
                    if (state == PlaybackState.Invisible) {
                        stopPlayback()
                    }
                }
            }

            launch {
                viewModel.loadingDeepLink.isLoading?.collectLatest {
                    binding.progressWheel?.visibility = if (it) View.VISIBLE else View.GONE
                }
            }

        }

        addOnPictureInPictureModeChangedListener {
            if (it.isInPictureInPictureMode) {
                viewModel.changePiPMode(true)
                layoutHandler?.forceFullScreen()
            } else {
                val last = viewModel.isInPIPMode.value
                if (last) {
                    viewModel.changePiPMode(false)
                    layoutHandler?.onStartLoading()
                }
            }
        }

        //Deeplink handle
        handleIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) {
            val shouldRecreate = when(newConfig.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> layoutHandler is LandscapeLayoutHandler
                Configuration.ORIENTATION_LANDSCAPE -> layoutHandler is PortraitLayoutHandler
                else -> false
            }
            if (shouldRecreate) {
                recreate()
            }
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!viewModel.isShowingPlayback.value) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this@ComplexActivity.enterPictureInPictureMode(params.build())
            } else {
                this@ComplexActivity.enterPictureInPictureMode()
            }
        }
    }

    private suspend fun handleAddSourceState(state: AddSourceState) {
        Log.d(TAG, "handleAddSourceState: $state")
        when(state) {
            is AddSourceState.StartLoad -> {
                binding.parseSourceLoadingContainer?.visibility = View.VISIBLE
                binding.parseSourceLoadingContainer?.invalidate()
                binding.statusView?.startLoading()
                binding.loadingDescription?.text = "Đang thêm nguồn: ${state.source.sourceUrl}..."
            }
            is AddSourceState.Success -> {
                binding.statusView?.showSuccess()
                binding.loadingDescription?.text = "Đã thêm nguồn: ${state.source.sourceUrl}"
            }
            is AddSourceState.Error -> {
                binding.statusView?.showError()
                binding.loadingDescription?.text = "Xảy ra lỗi"
            }
            else -> {
                binding.parseSourceLoadingContainer?.visibility = View.GONE
                binding.loadingDescription?.text = ""
            }
        }
    }
    private fun loadPlayback(data: PrepareStreamLinkData) {
        Log.d(TAG, "loadPlayback: $data")
        when(data) {
            is PrepareStreamLinkData.TV -> TVPlaybackFragment.newInstance(data.data)
            is PrepareStreamLinkData.IPTV -> IPTVPlaybackFragment.newInstance(data.data, data.configId, data.data.tvGroup)
            is PrepareStreamLinkData.Radio -> RadioPlaybackFragment.newInstance(data.data)
            is PrepareStreamLinkData.Football -> FootballPlaybackFragment.newInstance(data.data)
            else -> null
        }?.apply {
            this.callback = playbackAction
        }?.run {
            if (!isVisible) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_playback, this, screenName)
                    .commit()
            }

            layoutHandler?.onStartLoading()
        }
    }

    private fun stopPlayback() {
            binding.fragmentContainerPlayback.getFragment<Fragment>().takeIf { it != null }
                ?.run {
                supportFragmentManager.beginTransaction()
                    .remove(this)
                    .commit()
            }
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()
    }
    override fun onStart() {
        Log.d(TAG, "onStart: ")
        super.onStart()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        super.onPause()
    }
    override fun onResume() {
        Log.d(TAG, "onResume: ")
        super.onResume()

//        MainScope().launch {
//            val last = viewModel.isInPIPMode.value
//            if (last) {
//                viewModel.changePiPMode(false)
//                layoutHandler?.onStartLoading()
//            }
//        }
    }


    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            if (layoutHandler?.onBackEvent() == true) {
                return
            }
            super.onBackPressed()
        }


    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        layoutHandler?.onTouchEvent(ev)
        touchListenerList.forEach {
            it.onDispatchTouchEvent(null, ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val deeplink = intent?.data ?: return


        if (arrayListOf(Constants.HOST_TV, Constants.HOST_RADIO).contains(deeplink.host)) {
            lifecycleScope.launch {
                viewModel.loadChannelDeepLinkJob(deeplink)
            }
        } else if (deeplink.host == Constants.HOST_IPTV && deeplink.lastPathSegment == "search") {
            lifecycleScope.launch {
                layoutHandler?.onCloseMinimal()
                viewModel.openSearch(deeplink)
            }
        }
        intent?.data = null
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
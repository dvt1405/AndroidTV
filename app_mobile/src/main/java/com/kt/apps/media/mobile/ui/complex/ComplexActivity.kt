package com.kt.apps.media.mobile.ui.complex

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.utils.TAG
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
import com.kt.apps.media.mobile.utils.RotateOrientationEventListener
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.trackJob
import com.kt.apps.media.mobile.viewmodels.ComplexInteractors
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private var backPressedTimestamp: Long = 0

    private var orientationEventListener: RotateOrientationEventListener? = null

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
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    if (layoutHandler?.onBackEvent() == true) {
                        return
                    }
                    if (backPressedTimestamp + 2000 > System.currentTimeMillis()) {
                        finish()
                    } else {
                        backPressedTimestamp = System.currentTimeMillis()
                        Toast.makeText(this@ComplexActivity.baseContext, resources.getString(com.kt.apps.core.R.string.double_back_to_finish_title), Toast.LENGTH_SHORT).show()

                    }

                }
            }
        })

        binding.fragmentContainerPlayback.getFragment<BasePlaybackFragment<*>>()?.apply {
            this.callback = playbackAction
        }

        if (resources.getBoolean(R.bool.is_landscape)) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).let {
                it.show(WindowInsetsCompat.Type.systemBars())
            }
        }

//        orientationEventListener = object: RotateOrientationEventListener(baseContext) {
//            override fun onChanged(lastOrientation: Int, orientation: Int) {
//                if (lastOrientation != orientation) {
//                    handleRotationChange(orientation)
//                }
//            }
//        }
      }

    override fun initAction(savedInstanceState: Bundle?) {
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
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                dismissAllDialog()
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
        Log.d(TAG, "onConfigurationChanged: $newConfig")
        handleRotationChange(when(newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> RotateOrientationEventListener.ORIENTATION_PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE -> RotateOrientationEventListener.ORIENTATION_LANDSCAPE
            else -> -1
        })
    }

    private fun handleRotationChange(newOrientation: Int) {
        Log.d(TAG, "handleRotationChange: $newOrientation")
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) {
            val shouldRecreate = when(newOrientation) {
                RotateOrientationEventListener.ORIENTATION_PORTRAIT -> layoutHandler is LandscapeLayoutHandler
                RotateOrientationEventListener.ORIENTATION_LANDSCAPE -> layoutHandler is PortraitLayoutHandler
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

    private fun dismissAllDialog() {
        fun dismissDialog(manager: FragmentManager) {
            manager.fragments.forEach {
                (it as? DialogFragment)?.run {
                    this.dismissAllowingStateLoss()
                } ?: kotlin.run {
                    dismissDialog(it.childFragmentManager)
                }
            }
        }
        dismissDialog(supportFragmentManager)
    }


    override fun onSaveInstanceState(outState: Bundle) {
//        orientationEventListener?.currentOrientation?.run {
//            outState.putInt(LAST_ORIENTATION, this)
//        }
        super.onSaveInstanceState(outState)
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
        backPressedTimestamp = 0
//        if (orientationEventListener?.canDetectOrientation() == true) {
//            orientationEventListener?.enable()
//        }
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
            lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->  showErrorDialog(titleText = "Đã xảy ra lỗi vui lòng thử lại sau") }) {
                viewModel.loadChannelDeepLinkJob(deeplink)
            }.trackJob(viewModel.loadingDeepLink)
        } else if (deeplink.host == Constants.HOST_IPTV && deeplink.lastPathSegment == "search") {
            lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->  showErrorDialog(titleText = "Đã xảy ra lỗi vui lòng thử lại sau") }) {
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
            content = "Thêm nguồn kênh thành công!\r\nVui lòng chờ trong giây lát!"
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

    companion object {
        const val LAST_ORIENTATION = "LAST_ORIENTATION"
    }
}
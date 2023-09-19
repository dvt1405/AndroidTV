package com.kt.apps.core.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.StrictMode
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.kt.apps.core.BuildConfig
import com.kt.apps.core.R
import com.kt.apps.core.base.receiver.NetworkChangeReceiver
import com.kt.apps.core.base.receiver.NetworkChangeReceiver.Companion.isNetworkAvailable
import com.kt.apps.core.base.receiver.NetworkChangeReceiver.Companion.registerNetworkChangeReceiver
import com.kt.apps.core.base.receiver.NetworkChangeReceiver.Companion.unregisterNetworkChangeReceiver
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.blurry.Blur
import com.kt.apps.core.utils.blurry.BlurFactor
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.core.utils.updateLocale
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

abstract class BaseActivity<T : ViewDataBinding> : FragmentActivity(), HasAndroidInjector {
    abstract val layoutRes: Int
    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun initAction(savedInstanceState: Bundle?)
    lateinit var binding: T
    private var doubleBackToFinish = false
    private val updateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appUpdateInfoTask by lazy { updateManager.appUpdateInfo }
    private val appUpdateListener by lazy {
        InstallStateUpdatedListener { state ->

            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val byteDownload = state.bytesDownloaded()
                    val totalBytesDownload = state.totalBytesToDownload()
                }
                InstallStatus.DOWNLOADED -> {

                }
                else -> {

                }
            }
        }
    }


    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any?>? {
        return androidInjector
    }

    private var networkChangeReceiver: NetworkChangeReceiver? = NetworkChangeReceiver.getInstance()

    private var isCheckedUpdate: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
        networkChangeReceiver?.let {
            registerNetworkChangeReceiver(it, object : NetworkChangeReceiver.OnNetworkChangeListener {
                override fun onChange(isOnline: Boolean) {
                    showInternetConnected(isOnline)
                }
            })
        }
        AndroidInjection.inject(this)
        window.decorView.setBackgroundColor(Color.WHITE)
        super.onCreate(savedInstanceState)
        updateLocale()
        isCheckedUpdate  = savedInstanceState?.getBoolean(IS_CHECK_UPDATE) ?: false
        appUpdateInfoTask.addOnSuccessListener { updateInfo ->
            if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && !isCheckedUpdate) {
                isCheckedUpdate = true
                updateManager.registerListener(appUpdateListener)
                updateManager.startUpdateFlowForResult(
                    updateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }

        binding = DataBindingUtil.setContentView(this, layoutRes)
        initView(savedInstanceState)
        initAction(savedInstanceState)
    }

    private fun showInternetConnected(isOnline: Boolean) {
        val viewGroup = findViewById<ViewGroup>(android.R.id.content)
        if (isOnline) {
            viewGroup.findViewById<TextView?>(R.id.no_network_title_view)?.let {
                it.setBackgroundColor(Color.GREEN)
                it.text = "Kết nối internet đã sẵn sàng"
                it.animate()
                    .setStartDelay(300L)
                    .translationY(it.measuredHeight.toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            viewGroup.findViewById<LinearLayout?>(R.id.no_network_view)?.let {
                                viewGroup.removeView(it)
                            }
                        }
                    })
            }
        } else {
            viewGroup.findViewById<LinearLayout?>(R.id.no_network_view)?.let {
            } ?: viewGroup.addView(
                LayoutInflater.from(this@BaseActivity)
                    .inflate(R.layout.base_no_network_view, null, false)
            )
        }
    }

    override fun onResume() {
        showInternetConnected(this.isNetworkAvailable())
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_CHECK_UPDATE, isCheckedUpdate)
        super.onSaveInstanceState(outState)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    fun doubleBackToFinish() {
        if (doubleBackToFinish) {
            finish()
        } else {
            Toast.makeText(
                this,
                "Nhấn back lần nữa để thoát",
                Toast.LENGTH_SHORT
            ).show()
        }
        doubleBackToFinish = true
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToFinish = false
        }, 2000)

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        currentFocus?.let { currentFocusView ->
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE && currentFocusView is EditText
                && !currentFocusView::class.java.name.startsWith("android.webkit.")
            ) {
                val sourceCoordinator = IntArray(2)
                currentFocusView.getLocationOnScreen(sourceCoordinator)
                val x = ev.rawX + currentFocusView.left - sourceCoordinator[0]
                val y = ev.rawY + currentFocusView.top - sourceCoordinator[1]
                if (x < currentFocusView.left || x > currentFocusView.right || y < currentFocusView.top || y > currentFocusView.bottom) {
                    currentFocusView.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                showSuccessDialog({}, "App update fail!")
            } else {
                updateManager.unregisterListener(appUpdateListener)
                showSuccessDialog({}, "App update success")
            }
        }
    }

    fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            binding.root,
            "An update has just been downloaded.",
            Snackbar.LENGTH_SHORT
        ).apply {
            setAction("RESTART") { updateManager.completeUpdate() }
            setActionTextColor(resources.getColor(com.kt.skeleton.R.color.white))
            show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Logger.d(this, message = "onKeyDown: $keyCode")
        val iKeyCodeHandler: IKeyCodeHandler = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is IKeyCodeHandler
            }?.let {
                it as IKeyCodeHandler
            }
            ?: supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is IKeyCodeHandler
                }?.let {
                    it as IKeyCodeHandler
                } ?: return super.onKeyDown(keyCode, event)

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                iKeyCodeHandler.onDpadCenter()
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                iKeyCodeHandler.onDpadDown()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                iKeyCodeHandler.onDpadUp()
            }

            KeyEvent.KEYCODE_DPAD_DOWN_LEFT, KeyEvent.KEYCODE_DPAD_LEFT -> {
                iKeyCodeHandler.onDpadLeft()
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                iKeyCodeHandler.onDpadRight()
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                iKeyCodeHandler.onKeyCodeVolumeDown()
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                iKeyCodeHandler.onKeyCodeVolumeUp()
            }

            KeyEvent.KEYCODE_BACK -> {
            }

            KeyEvent.KEYCODE_CHANNEL_UP -> {
                iKeyCodeHandler.onKeyCodeChannelUp()
                return true
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                iKeyCodeHandler.onKeyCodeChannelDown()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                iKeyCodeHandler.onKeyCodeMediaNext()
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                iKeyCodeHandler.onKeyCodeMediaPrevious()
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                iKeyCodeHandler.onKeyCodePause()
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                iKeyCodeHandler.onKeyCodePlay()
            }

            KeyEvent.KEYCODE_INFO -> {

            }

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                iKeyCodeHandler.takeIf {
                    it is IMediaKeycodeHandler
                }?.let {
                    (it as IMediaKeycodeHandler).onKeyCodeForward()
                }
            }

            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                iKeyCodeHandler.takeIf {
                    it is IMediaKeycodeHandler
                }?.let {
                    (it as IMediaKeycodeHandler).onKeyCodeRewind()
                }
            }


            KeyEvent.KEYCODE_MENU -> {
                iKeyCodeHandler.onKeyCodeMenu()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        val fragment: BasePlaybackFragment = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is BasePlaybackFragment
            }?.let {
                it as BasePlaybackFragment
            } ?: return super.onBackPressed()
        if (fragment.canBackToMain()) {
            Logger.d(
                this, "BackPressed", message = "" +
                        "{" +
                        "className: ${this.componentName.className}, " +
                        "isTaskRoot: $isTaskRoot," +
                        "activityCount: ${CoreApp.activityCount}" +
                        "}"
            )
            if (this.componentName.flattenToString().contains("PlaybackActivity") && CoreApp.activityCount == 1) {
                startActivity(Intent().apply {
                    this.data = Uri.parse("xemtv://tv/dashboard")
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } else {
                finish()
            }
        } else {
            fragment.hideOverlay()
        }
    }

    fun setBackgroundOverlay() {
        BackgroundManager.getInstance(this).apply {
            attach(window)
            val bg: Bitmap = BitmapFactory.decodeResource(
                this@BaseActivity.resources,
                com.kt.apps.resources.R.drawable.bg_tv
            )
            drawable = BitmapDrawable(null, Blur.of(this@BaseActivity,
                bg,
                BlurFactor().apply {
                    this.radius = 10
                    this.sampling = 1
                }
            ))
        }
    }

    override fun onDestroy() {
        networkChangeReceiver?.let {
            unregisterNetworkChangeReceiver(it)
            networkChangeReceiver = null
        }
        super.onDestroy()
    }

    companion object {
        private const val UPDATE_REQUEST_CODE = 102
        private const val IS_CHECK_UPDATE = "IS_CHECK_UPDATE"
    }
}

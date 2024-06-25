package com.kt.apps.media.xemtv.ui.main

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.autoupdate.ui.FragmentInfo
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.getIsVipDb
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.saveIsVipDb
import com.kt.apps.media.xemtv.BuildConfig
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityMainBinding
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.extensions.ExtensionsViewModel
import com.kt.apps.media.xemtv.ui.favorite.FavoriteViewModel
import com.kt.apps.media.xemtv.ui.search.SearchViewModels
import javax.inject.Inject

/**
 * Loads [DashboardFragment].
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var roomDataBase: RoomDataBase

    @Inject
    lateinit var keyValueStorage: IKeyValueStorage

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    private val favoriteViewModel by lazy {
        ViewModelProvider(this, factory)[FavoriteViewModel::class.java]
    }

    private val extensionsViewModel by lazy {
        ViewModelProvider(this, factory)[ExtensionsViewModel::class.java]
    }

    private val searchViewModel by lazy {
        ViewModelProvider(this, factory)[SearchViewModels::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("TAG", "ONCreate", "${intent.action} - ${intent.extras}")
        if (BuildConfig.isBeta && !keyValueStorage.getIsVipDb()) {
            keyValueStorage.saveIsVipDb(true)
        }
        askNotificationPermission()
    }
    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Logger.d("TAG", "askNotificationPermission", "PERMISSION_GRANTED")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Logger.d("TAG", "askNotificationPermission", "PERMISSION_DENIED")
            } else {
                Logger.d("TAG", "askNotificationPermission", "PERMISSION_DENIED")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Logger.d("TAG", "onResume", "${intent.action} - ${intent.extras}")
    }
    override fun initView(savedInstanceState: Bundle?) {
        Logger.d(this, "initView", "${intent.action} - ${intent.extras}")
        screenWidth = binding.root.measuredWidth
    }

    override fun initAction(savedInstanceState: Bundle?) {
        Logger.d(this, "initAction", "${intent.action} - ${intent.extras}")
        handleSearch(intent)
        FragmentInfo.appVersion = BuildConfig.VERSION_NAME
        searchViewModel.querySearch("")
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment, DashboardFragment())
            .commitNow()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
            .takeIf {
                it is DashboardFragment
            }?.let {
                it as DashboardFragment
            }?.let {
                if (it.disableFocusSearch
                    && keyCode != KeyEvent.KEYCODE_BACK
                ) {
                    return true
                }
            }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDialogDismiss() {
        super.onDialogDismiss()
        supportFragmentManager
            .findFragmentById(R.id.main_browse_fragment)
            .takeIf { it is DashboardFragment }
            ?.let {
                it as DashboardFragment
            }?.invalidateNavDrawerSelectedPosition()
    }

    override fun onDialogShowing() {
        super.onDialogShowing()
        supportFragmentManager
            .findFragmentById(R.id.main_browse_fragment)
            .takeIf { it is DashboardFragment }
            ?.let {
                it as DashboardFragment
            }?.invalidateNavDrawerSelectedPosition()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is DashboardFragment
                }?.let {
                    (it as DashboardFragment).selectPageRowByUri(uri)
                }
        }
        handleSearch(intent)
    }

    private fun handleSearch(intent: Intent?) {
        if (Intent.ACTION_SEARCH == intent?.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                searchViewModel.querySearch(query)
            }
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentById(android.R.id.content)
            ?.let {
                super.onBackPressed()
            }
            ?: supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is DashboardFragment
                }?.let {
                    (it as DashboardFragment).apply {
                        this.onBackPressed()
                    }
                }
            ?: super.onBackPressed()

    }

    companion object {
        var screenWidth = -1
    }
}
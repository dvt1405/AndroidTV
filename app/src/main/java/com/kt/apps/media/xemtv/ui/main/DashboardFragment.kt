package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.app.*
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.kt.apps.autoupdate.ui.FragmentQrCode
import com.kt.apps.core.Constants
import com.kt.apps.core.R
import com.kt.apps.core.base.IKeyCodeHandler
import com.kt.apps.core.base.leanback.*
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.base.leanback.NavDrawerView.INavDrawerItemSelected
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.leanback.findCurrentFocusedPosition
import com.kt.apps.core.utils.leanback.findCurrentFocusedView
import com.kt.apps.core.utils.leanback.findCurrentSelectedPosition
import com.kt.apps.media.xemtv.BuildConfig
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.extensions.ExtensionsViewModel
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions
import com.kt.apps.media.xemtv.ui.extensions.FragmentDashboardExtensions
import com.kt.apps.media.xemtv.ui.favorite.FavoriteViewModel
import com.kt.apps.media.xemtv.ui.search.SearchViewModels
import com.kt.apps.media.xemtv.ui.search.TVSearchFragment
import com.kt.apps.media.xemtv.ui.tv.BaseTabLayoutFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboardNew
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class DashboardFragment : BrowseSupportFragment(), HasAndroidInjector, IKeyCodeHandler {

    private lateinit var mBackgroundManager: BackgroundManager

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var roomDataBase: RoomDataBase

    private var currentPageIdSelected: Long = -1

    private val rowsAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private val searchViewModels by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[SearchViewModels::class.java]
    }
    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }
    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[ExtensionsViewModel::class.java]
    }
    private val favoriteViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[FavoriteViewModel::class.java]
    }
    val disableFocusSearch: Boolean
        get() = if (mMainFragment is FragmentTVDashboardNew) {
            (mMainFragment as FragmentTVDashboardNew).isProgressShowing()
        } else {
            false
        }

    private val childFocusSearchListener by lazy {
        object : BrowseFrameLayout.OnFocusSearchListener {
            override fun onFocusSearch(focused: View?, direction: Int): View? {
                Logger.d(
                    this@DashboardFragment,
                    "FocusSearch",
                    "{direction:$direction, focused: $focused}"
                )
                if (mMainFragment is FragmentDashboardExtensions) {
                    try {
                        return (mMainFragment as FragmentDashboardExtensions).onFocusSearch(
                            focused,
                            direction
                        )
                    } catch (_: Throwable) {
                    }
                }

                if (mMainFragment is BaseTabLayoutFragment
                    && focused is TabLayout.TabView
                    && direction == View.FOCUS_UP
                ) {
                    return focused
                }

                if (focused is DashboardTVChannelPresenter.TVImageCardView
                    && direction == View.FOCUS_UP
                    && mMainFragment is BaseTabLayoutFragment
                ) {
                    (mMainFragment as BaseTabLayoutFragment).apply {
                        return this.tabLayout?.getTabAt(this.currentPage)?.view
                    }
                } else if (mMainFragment is BaseTabLayoutFragment
                    && focused is TabLayout.TabView
                    && direction == View.FOCUS_LEFT
                ) {
                    val tabCount = (mMainFragment as BaseTabLayoutFragment).tabLayout?.tabCount ?: 0
                    val tabFocused = (mMainFragment as BaseTabLayoutFragment).tabLayout!!
                        .findCurrentFocusedPosition()
                    if (tabFocused > 0) {
                        return (mMainFragment as BaseTabLayoutFragment).tabLayout!!
                            .getTabAt((tabFocused - 1) % tabCount)!!.view
                    }
                } else if (mMainFragment is BaseTabLayoutFragment
                    && focused is TabLayout.TabView
                    && direction == View.FOCUS_RIGHT
                ) {
                    val tabCount = (mMainFragment as BaseTabLayoutFragment).tabLayout!!.tabCount
                    val tabFocused = (mMainFragment as BaseTabLayoutFragment).tabLayout!!
                        .findCurrentFocusedPosition()
                    if (tabFocused == tabCount - 1) {
                        return focused
                    }
                }

                if (mMainFragment is BaseTabLayoutFragment
                    && focused is TabLayout.TabView
                    && direction == View.FOCUS_DOWN
                ) {
                    return (mMainFragment as BaseTabLayoutFragment).requestFocusChildContent()
                }
                return this@DashboardFragment.onFocusSearchListener.onFocusSearch(focused, direction)
            }
        }
    }

    private val pageRowFactory by lazy {
        DashboardPageRowFactory(BackgroundManager.getInstance(requireActivity()))
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        progressBarManager.disableProgressBar()
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainFragmentRegistry.registerFragment(
            PageRow::class.java,
            pageRowFactory
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initView()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAction()
        mBrowseFrame.onFocusSearchListener = childFocusSearchListener
    }

    private fun initView() {
        headersState = HEADERS_ENABLED
        arguments = arguments ?: bundleOf()
        requireArguments().putInt(ARG_HEADERS_STATE, HEADERS_ENABLED)
        isHeadersTransitionOnBackEnabled = true
        prepareEntranceTransition()
        adapter = rowsAdapter
    }

    private fun initAction() {
        var lastSelectedItem = -1
        navDrawerView.onNavDrawerItemSelected = object : INavDrawerItemSelected {
            override fun onSelected(position: Int, itemSelected: Int) {
                if (position == defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_SEARCH)) {
                    if (lastSelectedItem != position) {
                        searchViewModels.queryDefaultSearch()
                    }
                } else {
                    searchViewModels.clearLastSelectedItem()
                }
                if (position != defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_FAVORITE)) {
                    favoriteViewModel.clearLastSelectedStreamingTask()
                }

                if (position == defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_TV)
                    || position == defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_RADIO)
                    || position == defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_FAVORITE)
                ) {
                    if (lastSelectedItem != position) {
                        tvChannelViewModel.cancelCurrentGetStreamLinkTask()
                        tvChannelViewModel.clearCurrentPlayingChannelState()
                    }
                }
                onRowSelected(position)
                lastSelectedItem = position
            }
        }

        Logger.d(this, message = "initAction")
        activity?.intent?.data?.let {
            selectPageRowByUri(it)
        }
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mBackgroundManager.color = Color.BLACK
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            Logger.d(this, tag = "DashboardSelected", message = row.toString())
            currentPageIdSelected = row.id
        }

        defaultPages.forEach {
            val header = DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                it.key,
                it.value,
                defaultPagesIcon[it.key]!!
            )
            val pageRow = PageRow(header)
            rowsAdapter.add(pageRow)
        }
        startEntranceTransition()
        Handler(Looper.getMainLooper()).postDelayed({
            navDrawerView.setCloseState()
        }, 200)
    }

    fun selectPageRowByUri(uri: Uri) {
        when (uri.scheme) {
            Constants.SCHEME_DEFAULT -> {
                Logger.d(this, message = uri.toString())
                when (uri.host) {
                    Constants.HOST_FOOTBALL -> {
                        try {
                            if (requireActivity().supportFragmentManager.findFragmentById(android.R.id.content) is FragmentQrCode) {
                                requireActivity().supportFragmentManager.popBackStackImmediate()
                            }
                        } catch (_: Exception) {
                        }

                        onRowSelected(defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_FOOTBALL))
                    }
                    Constants.HOST_TV -> {
                        try {
                            if (requireActivity().supportFragmentManager.findFragmentById(android.R.id.content) is FragmentQrCode) {
                                requireActivity().supportFragmentManager.popBackStackImmediate()
                            }
                        } catch (_: Exception) {
                        }

                        onRowSelected(defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_TV))
                    }
                    Constants.HOST_RADIO -> {
                        try {
                            if (requireActivity().supportFragmentManager.findFragmentById(android.R.id.content) is FragmentQrCode) {
                                requireActivity().supportFragmentManager.popBackStackImmediate()
                            }
                        } catch (_: Exception) {
                        }

                        onRowSelected(defaultPages.keys.indexOf(DashboardPageRowFactory.ROW_RADIO))
                    }
                }
            }

            else -> {

            }
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onResume() {
        super.onResume()
        if (mMainFragment?.isDetached == false) {
            if (mMainFragment is BaseTabLayoutFragment) {
                (mMainFragment as BaseTabLayoutFragment)
                    .requestFocusChildContent()
                    ?.requestFocus()
            } else if (mMainFragment is TVSearchFragment) {
                (mMainFragment as TVSearchFragment).verticalGridView.requestFocus()
            }
            if (mMainFragment !is TVSearchFragment) {
                searchViewModels.queryDefaultSearch()
            } else if (mMainFragment is FragmentDashboardExtensions) {
                searchViewModels.queryDefaultSearch(true)
            }
            if (selectedPosition >= 0) {
                navDrawerView.setEnableSelectedItem(selectedPosition, true)
            }
        }
        mBackgroundManager.drawable = when (currentPageIdSelected) {
            DashboardPageRowFactory.ROW_FOOTBALL -> {
                ContextCompat.getDrawable(requireContext(), R.drawable.main_background)
            }

            else -> {
                ContextCompat.getDrawable(requireContext(), R.drawable.tv_bg)
            }
        }
    }
    override fun onDestroyView() {
        firstInit = true
        super.onDestroyView()
    }

    override fun onDpadCenter() {

        if (mainFragment is FragmentAddExtensions) {
            (mainFragment as FragmentAddExtensions)
                .onDpadCenter()
        }
    }

    override fun onDpadDown() {

    }

    override fun onPause() {
        super.onPause()
        searchViewModels.clearLastSelectedItem()
//        navDrawerView.setItemSelected(selectedPosition, true)
    }

    override fun onDpadUp() {
        if (mainFragment is FragmentAddExtensions) {
            (mainFragment as FragmentAddExtensions)
                .onDpadUp()
        }
    }

    override fun onDpadLeft() {

    }

    override fun onDpadRight() {

    }

    override fun onKeyCodeChannelUp() {

    }

    override fun onKeyCodeChannelDown() {

    }

    override fun onKeyCodeMediaPrevious() {

    }

    override fun onKeyCodeMediaNext() {

    }

    override fun onKeyCodeVolumeUp() {

    }

    override fun onKeyCodeVolumeDown() {

    }

    override fun onKeyCodePause() {

    }

    override fun onKeyCodePlay() {

    }

    override fun onKeyCodeMenu() {

    }

    private var _allowFinish = false

    fun onBackPressed() {
        if (mMainFragment is BaseTabLayoutFragment) {
            with(mMainFragment as BaseTabLayoutFragment) {
                if (this is FragmentDashboardExtensions) {
                    if (!isShowingHeaders && this.btnAddSource?.isFocused == true) {
                        navDrawerView.requestFocus()
                        return
                    }
                }
                if (this is FragmentTVDashboardNew) {
                    if (this.isProgressShowing()) {
                        this.dismissProgressAndCancelCurrentTask()
                        return
                    }
                }
                if (isShowingHeaders) {
                    finishActivityIfNeeded()
                } else {
                    val currentTabFocused = this.tabLayout?.findCurrentFocusedView()
                    if (currentTabFocused == null) {
                        val currentTabSelected = this.tabLayout?.findCurrentSelectedPosition() ?: -1
                        if (currentTabSelected >= 0) {
                            this.tabLayout!!.getTabAt(currentTabSelected)?.view?.requestFocus()
                        } else {
                            navDrawerView.openNav()
                        }
                    } else {
                        navDrawerView.requestFocus()
                    }
                }
            }
        } else if (isShowingHeaders) {
            finishActivityIfNeeded()
        } else {
            navDrawerView.requestFocus()
        }
    }

    private fun finishActivityIfNeeded() {
        Handler(Looper.getMainLooper()).postDelayed({
            _allowFinish = false
        }, 3000)
        if (_allowFinish) {
            requireActivity().finish()
        } else {
            Toast.makeText(requireContext(), "Nhấn back lần nữa để thoát ứng dụng", Toast.LENGTH_SHORT)
                .show()
            _allowFinish = true
        }
    }

    fun invalidateNavDrawerSelectedPosition() {
        if (selectedPosition < 0) {
            return
        }
        Logger.d(this, message = "selectedPosition: $selectedPosition")
        navDrawerView.setItemSelected(selectedPosition, true)
    }
    companion object {
        var firstInit = true
        private const val EXTRA_EXTERNAL_EXTENSIONS = "extra:external"
        val defaultPages by lazy {
            if (BuildConfig.isBeta) {
                mapOf(
                    DashboardPageRowFactory.ROW_SEARCH to "Tìm kiếm",
                    DashboardPageRowFactory.ROW_FAVORITE to "Yêu thích",
                    DashboardPageRowFactory.ROW_TV to "Truyền hình",
                    DashboardPageRowFactory.ROW_RADIO to "Phát thanh",
                    DashboardPageRowFactory.ROW_FOOTBALL to "Bóng đá",
                    DashboardPageRowFactory.ROW_IPTV to "IPTV",
                    DashboardPageRowFactory.ROW_INFO to "Thông tin"
                )
            } else {
                mapOf(
                    DashboardPageRowFactory.ROW_SEARCH to "Tìm kiếm",
                    DashboardPageRowFactory.ROW_FAVORITE to "Yêu thích",
                    DashboardPageRowFactory.ROW_TV to "Truyền hình",
                    DashboardPageRowFactory.ROW_RADIO to "Phát thanh",
                    DashboardPageRowFactory.ROW_IPTV to "IPTV",
                    DashboardPageRowFactory.ROW_INFO to "Thông tin"
                )
            }
        }
        private val defaultPagesIcon by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_SEARCH to com.kt.apps.resources.R.drawable.ic_search,
                DashboardPageRowFactory.ROW_FAVORITE to com.kt.apps.resources.R.drawable.ic_round_bookmark_border_24,
                DashboardPageRowFactory.ROW_TV to com.kt.apps.resources.R.drawable.ic_tv,
                DashboardPageRowFactory.ROW_FOOTBALL to R.drawable.ic_soccer_ball,
                DashboardPageRowFactory.ROW_RADIO to com.kt.apps.resources.R.drawable.ic_radio,
                DashboardPageRowFactory.ROW_ADD_EXTENSION to com.kt.apps.media.xemtv.R.drawable.round_add_circle_outline_24,
                DashboardPageRowFactory.ROW_IPTV to com.kt.apps.resources.R.drawable.ic_iptv,
                DashboardPageRowFactory.ROW_INFO to R.drawable.ic_outline_info_24,
            )
        }

        fun newInstance(extensionsConfigs: List<ExtensionsConfig>) = DashboardFragment().apply {
            arguments = bundleOf(
                EXTRA_EXTERNAL_EXTENSIONS to extensionsConfigs.toTypedArray()
            )
        }
    }
}
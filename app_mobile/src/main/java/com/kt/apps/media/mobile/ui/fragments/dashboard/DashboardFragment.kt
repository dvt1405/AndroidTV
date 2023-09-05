package com.kt.apps.media.mobile.ui.fragments.dashboard

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.PopupWindow
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.ListPopupWindow
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentDashboardBinding
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.DashboardPagerAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.search.SearchDashboardFragment
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.screenWidth
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class DashboardFragment : BaseMobileFragment<FragmentDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_dashboard

    override val screenName: String
        get() = "DashboardFragment"
    private val _adapter by lazy {
        DashboardPagerAdapter(this)
    }

    private val uiControlViewModel by lazy {
        ViewModelProvider(this.requireActivity(), factory)[UIControlViewModel::class.java]
    }

    private val navigationBar by lazy {
        binding.bottomNavigation as NavigationBarView
    }

    private val popupMenu by lazy {
        val view = navigationBar.findViewById<View>(R.id.more)
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.popup_navigation, menu)
            setOnMenuItemClickListener {popupItem ->
                binding.viewpager.setCurrentItem(_adapter.getPositionForItem(popupItem.itemId), false)
                true
            }
        }
    }

    private val onItemSelectedListener: OnItemSelectedListener = OnItemSelectedListener {
        if (!isLandscape) {
            if (it.itemId == R.id.more) {
                this@DashboardFragment.popupMenu.show()
                return@OnItemSelectedListener true
            }
            if (it.itemId == R.id.search) {
                showSearchPopup()
                return@OnItemSelectedListener true
            }
            binding.viewpager.setCurrentItem(_adapter.getPositionForItem(it.itemId), false)
            return@OnItemSelectedListener true
        } else {
            binding.viewpager.setCurrentItem(_adapter.getPositionForItem(it.itemId), false)
            return@OnItemSelectedListener true
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding.viewpager) {
            adapter = _adapter
            isUserInputEnabled = false
        }
        (binding.bottomNavigation as NavigationBarView).apply {
            labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        if (!isLandscape) {
            val popupMenu = popupMenu.menu
            if (BuildConfig.isBeta) {
                popupMenu.findItem(R.id.football).isVisible = true
            }
            val navigationMenu = (binding.bottomNavigation as NavigationBarView).menu
            _adapter.onRefresh((navigationMenu.children.toList() + popupMenu.children.toList())
                .filter { it.isVisible }
                .map {
                it.itemId
            }.asSequence())
        } else {
            if (BuildConfig.isBeta) {
                (binding.bottomNavigation as NavigationBarView).menu.findItem(R.id.football).isVisible =
                    true
            }
            _adapter.onRefresh((binding.bottomNavigation as NavigationBarView).menu.children
                .filter { it.isVisible }
                .map {
                it.itemId
            })
        }

        (binding.bottomNavigation as NavigationBarView).setOnItemSelectedListener(onItemSelectedListener)

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                uiControlViewModel.openSearchEvent.collectLatest {
                    (binding.bottomNavigation as NavigationBarView).selectedItemId = R.id.search
                }
            }
        }

        (binding.bottomNavigation as NavigationBarView).selectedItemId = R.id.tv
    }

    override fun onDestroyView() {
        binding.viewpager.adapter = null
        super.onDestroyView()
    }
    override fun onDetach() {
        super.onDetach()
        (binding.bottomNavigation as NavigationBarView).setOnItemSelectedListener(null)
        binding.viewpager.adapter = null
    }
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.viewpager.currentItem.let { _adapter.getItemOrNull(it) }
            ?.run {
                silentSelectNavigationBar(
                    if (isLandscape) {
                        this
                    } else {
                        val popupMenu = popupMenu.menu.children.toList()
                        popupMenu.firstOrNull { it.itemId == this }?.let {
                            R.id.more
                        } ?: this
                    }
                )

            }
    }

    private fun silentSelectNavigationBar(item: Int) {
        (binding.bottomNavigation as NavigationBarView).run {
            this.setOnItemSelectedListener(null)
            selectedItemId = item
            this.setOnItemSelectedListener(onItemSelectedListener)
        }
    }
    private fun showSearchPopup() {
        val searchFragment = SearchDashboardFragment()

        this.activity?.supportFragmentManager?.beginTransaction()
            ?.add(android.R.id.content, searchFragment)
            ?.addToBackStack(searchFragment.screenName)
            ?.commit()
    }

    companion object {
        private const val SELECTED_ID = "selected_id"
        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            return fragment
        }
    }
}
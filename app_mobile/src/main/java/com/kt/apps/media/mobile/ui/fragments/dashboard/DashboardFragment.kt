package com.kt.apps.media.mobile.ui.fragments.dashboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationBarView
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.DashboardPagerAdapter
import com.kt.apps.media.mobile.ui.fragments.search.SearchDashboardFragment
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
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
        DashboardPagerAdapter(requireActivity())
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
            val navigationMenu = (binding.bottomNavigation as NavigationBarView).menu
            _adapter.onRefresh((popupMenu.children.toList() + navigationMenu.children.toList()).map {
                it.itemId
            }.asSequence())

            (binding.bottomNavigation as NavigationBarView).setOnItemSelectedListener {
                if (it.itemId == R.id.more) {
                    this@DashboardFragment.popupMenu.show()
                    return@setOnItemSelectedListener  true
                }
                if (it.itemId == R.id.search) {
//                    this.binding.searchFragmentContainer?.fadeIn {  }
                    showSearchPopup()
                    return@setOnItemSelectedListener true
                }
//                binding.searchFragmentContainer?.fadeOut {  }
                binding.viewpager.setCurrentItem(_adapter.getPositionForItem(it.itemId), false)
                return@setOnItemSelectedListener true
            }
        } else {
            _adapter.onRefresh((binding.bottomNavigation as NavigationBarView).menu.children.map {
                it.itemId
            })

            (binding.bottomNavigation as NavigationBarView).setOnItemSelectedListener {
                binding.viewpager.setCurrentItem(_adapter.getPositionForItem(it.itemId), false)
                return@setOnItemSelectedListener true
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                uiControlViewModel.openSearchEvent.collectLatest {
                    (binding.bottomNavigation as NavigationBarView).selectedItemId = R.id.search
                }
            }
        }

        (binding.bottomNavigation as NavigationBarView).selectedItemId = R.id.tv
    }

    private fun showSearchPopup() {
        val searchFragment = SearchDashboardFragment()

        this.activity?.supportFragmentManager?.beginTransaction()
            ?.add(android.R.id.content, searchFragment)
            ?.addToBackStack(searchFragment.screenName)
            ?.commit()

    }
}
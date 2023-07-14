package com.kt.apps.media.mobile.ui.fragments.dashboard

import android.os.Bundle
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationBarMenu
import com.google.android.material.navigation.NavigationBarView
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.DashboardPagerAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.models.WrappedTVChannelViewModel
import javax.inject.Inject

class DashboardFragment : BaseFragment<FragmentDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_dashboard

    private val tvViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    override val screenName: String
        get() = "DashboardFragment"
    private val _adapter by lazy {
        DashboardPagerAdapter(requireActivity())
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.viewpager.adapter = _adapter
        binding.viewpager.isUserInputEnabled = false
        (binding.bottomNavigation as NavigationBarView).apply {
            labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {

        _adapter.onRefresh((binding.bottomNavigation as NavigationBarView).menu.children.map {
            it.itemId
        })
        (binding.bottomNavigation as NavigationBarView).setOnItemSelectedListener {
            binding.viewpager.setCurrentItem(_adapter.getPositionForItem(it.itemId), false)
            return@setOnItemSelectedListener true
        }

        tvViewModel.getListTVChannel(false)
    }
}
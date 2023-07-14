package com.kt.apps.media.mobile.ui.fragments.tv

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.type.Color
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.IDashboardHelper
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVDashboardAdapter
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class FragmentTVDashboard(private val helper: IDashboardHelper) : BaseFragment<FragmentTvDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    override val layoutResId: Int
        get() = R.layout.fragment_tv_dashboard
    override val screenName: String
        get() = "FragmentTVDashboard"

    private val _adapter by lazy {
        TVDashboardAdapter(this, helper)
    }

    private val tvViewModel by lazy {
        helper.wrapViewModel(ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java])
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.viewpager.adapter = _adapter
        binding.viewpager.isUserInputEnabled = false

        TabLayoutMediator(
            binding.tabLayout, binding.viewpager, true
        ) { tab, position ->
            tab.text = _adapter.getTitleForPage(position)
        }.attach()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initAction(savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            tvViewModel.listChannels.collectLatest {
                Log.d(TAG, "initAction: $it")
            }
        }
        lifecycleScope.launchWhenCreated {
            tvViewModel.groupTVChannel.mapLatest {
                it.keys
            }.collectLatest {
                _adapter.onRefresh(it)
            }
        }

    }
}
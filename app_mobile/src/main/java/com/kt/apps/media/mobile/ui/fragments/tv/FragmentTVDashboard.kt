package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVDashboardAdapter
import com.kt.skeleton.KunSkeleton
import javax.inject.Inject

class FragmentTVDashboard : BaseFragment<FragmentTvDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_tv_dashboard
    override val screenName: String
        get() = "FragmentTVDashboard"

    private val tvViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    private val _adapter by lazy {
        TVDashboardAdapter(this)
    }

    private val skeleton by lazy {
        KunSkeleton.bind(binding.viewpager, binding.tabLayout, this)
            .fragmentItemLayout(R.layout.item_channel_skeleton)
            .build()

    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.viewpager.adapter = _adapter
    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvViewModel.wrapperListChannel.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    _adapter.onRefresh(it.data.keys)
                    TabLayoutMediator(
                        binding.tabLayout, binding.viewpager, true
                    ) { tab, position ->
                        tab.text = _adapter.getTitleForPage(position)
                    }.attach()
                }

                is DataState.Loading -> {
//                    skeleton.run()
                }

                is DataState.Error -> {

                }

                else -> {

                }
            }
        }

    }
}
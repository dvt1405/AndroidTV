package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.IDashboardHelper
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.RadioDashboardHelper
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.TVDashboardHelper
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class FragmentTVDashboard : BaseFragment<FragmentTvDashboardBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    override val layoutResId: Int
        get() = R.layout.fragment_tv_dashboard
    override val screenName: String
        get() = "FragmentTVDashboard"

    private val _adapter by lazy {
        TVDashboardAdapter(this, helper)
    }

    private val helper: IDashboardHelper by lazy {
        if (arguments?.get(isRadioExtra) as? Boolean == true)
            RadioDashboardHelper()
        else
            TVDashboardHelper()
    }

    private val tvViewModel by lazy {
        helper.wrapViewModel(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }

    override fun initView(savedInstanceState: Bundle?) {

        with(binding.viewpager) {
            adapter = _adapter
            isUserInputEnabled = false
            offscreenPageLimit = 1
            setPageTransformer   { page, position ->
                page.alpha = 0f
                page.visibility = View.VISIBLE
                page.animate()
                    .alpha(1f)
                    .setDuration(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
            }
        }

        TabLayoutMediator(
            binding.tabLayout, binding.viewpager, true, false
        ) { tab, position ->
            tab.text = _adapter.getTitleForPage(position)
        }.attach()
    }

    override fun onDestroyView() {
        binding.viewpager.adapter = null
        super.onDestroyView()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initAction(savedInstanceState: Bundle?) {
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                tvViewModel.groupTVChannel.mapLatest {
                    it.keys
                }.collectLatest {
                    _adapter.onRefresh(it)
                }
            }
        }
    }

    companion object {
        const val isRadioExtra = "extra:isRadio"
        const val TAG = "FragmentTVDashboard"
        private const val currentItem = "CURRENT_ITEM"
        fun newInstance(isRadio: Boolean): FragmentTVDashboard {

            val fragment = FragmentTVDashboard().apply {
                arguments = bundleOf(
                    isRadioExtra to isRadio
                )
            }
            return fragment
        }
    }
}

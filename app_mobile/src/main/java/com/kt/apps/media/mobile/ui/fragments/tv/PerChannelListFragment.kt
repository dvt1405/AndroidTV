package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvChannelListBinding
import com.kt.apps.media.mobile.ui.fragments.BaseMobileFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.repeatLaunchsOnLifeCycle
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentInteractors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class PerChannelListFragment : BaseMobileFragment<FragmentTvChannelListBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_tv_channel_list
    override val screenName: String
        get() = "FragmentTVChannelList"

    abstract val interactors: ChannelFragmentInteractors

    private val filterCategory by lazy {
        requireArguments().getString(EXTRA_TV_CHANNEL_CATEGORY)!!
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.verticalRecyclerView.changeDisplayStyle(ChannelListView.DisplayStyle.FLEX)
    }

    override fun initAction(savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            interactors.groupTVChannel.map {
                it[filterCategory]
            }
                .map { it?.map { tvChannel -> ChannelElement.TVChannelElement(tvChannel) } }
                .collectLatest { binding.verticalRecyclerView.reloadAllData(it ?: emptyList() )}
        }

        if (isLandscape) {
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    interactors.onMinimalPlayer.collectLatest {
                        with(binding.verticalRecyclerView) {
                            if (it) {
                                setPadding(0, 0, 0, (screenHeight * 0.5).toInt())
                                clipToPadding = false
                            } else {
                                setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        internal const val EXTRA_TV_CHANNEL_CATEGORY = "extra:tv_channel_category"
//        fun newInstance(filterCategory: String): PerChannelListFragment {
//            return PerChannelListFragment().apply {
//                arguments = bundleOf(
//                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
//                )
//            }
//        }
    }
}
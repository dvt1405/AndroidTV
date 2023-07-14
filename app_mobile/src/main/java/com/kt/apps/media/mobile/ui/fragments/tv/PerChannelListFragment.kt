package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvChannelListBinding
import com.kt.apps.media.mobile.ui.fragments.models.ChannelsModelAdapter
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVChannelListAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

abstract class PerChannelListFragment : BaseFragment<FragmentTvChannelListBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_tv_channel_list
    override val screenName: String
        get() = "FragmentTVChannelList"

    abstract val tvViewModel: ChannelsModelAdapter

    private val _adapter by lazy {
        TVChannelListAdapter()
    }

    private val filterCategory by lazy {
        requireArguments().getString(EXTRA_TV_CHANNEL_CATEGORY)!!
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding.verticalRecyclerView) {
            adapter = _adapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
            doOnPreDraw {
                val spanCount = 3.coerceAtLeast((measuredWidth / 220.dpToPx()))
                layoutManager = GridLayoutManager(requireContext(), spanCount)
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            tvViewModel.groupTVChannel.map {
                it[filterCategory]
            }.collectLatest {
                _adapter.onRefresh(it ?: emptyList())
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
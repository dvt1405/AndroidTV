package com.kt.apps.media.mobile.ui.fragments.tv

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignContent
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvChannelListBinding
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVChannelListAdapter
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
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

    abstract val tvViewModel: ChannelFragmentViewModel

    private val _adapter by lazy {
        TVChannelListAdapter()
    }

    private val filterCategory by lazy {
        requireArguments().getString(EXTRA_TV_CHANNEL_CATEGORY)!!
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding.verticalRecyclerView) {
            adapter = _adapter
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                isItemPrefetchEnabled = true
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
            setHasFixedSize(true)
            addItemDecoration(channelItemDecoration)
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
package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvChannelListBinding
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVChannelListAdapter
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVChannelsHorizontalAdapter
import javax.inject.Inject

class FragmentTVChannelList : BaseFragment<FragmentTvChannelListBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_tv_channel_list
    override val screenName: String
        get() = "FragmentTVChannelList"

    private val tvViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    private val _adapter by lazy {
        TVChannelListAdapter()
    }

    private val filterCategory by lazy {
        requireArguments().getString(EXTRA_TV_CHANNEL_CATEGORY)!!
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.verticalRecyclerView.adapter = _adapter
        binding.verticalRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvViewModel.wrapperListChannel.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    it.data[filterCategory]?.let {
                        _adapter.onRefresh(it)
                    }
                }

                is DataState.Loading -> {

                }

                else -> {

                }
            }
        }
    }

    companion object {
        private const val EXTRA_TV_CHANNEL_CATEGORY = "extra:tv_channel_category"
        fun newInstance(filterCategory: String): FragmentTVChannelList {
            return FragmentTVChannelList().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}
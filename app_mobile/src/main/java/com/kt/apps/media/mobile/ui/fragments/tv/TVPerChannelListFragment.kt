package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.RadioChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentViewModel
import com.kt.apps.media.mobile.viewmodels.features.loadLinkStreamChannel
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class TVPerChannelListFragment : PerChannelListFragment() {
    private val _interactors by lazy {
        TVChannelFragmentViewModel(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val interactors: ChannelFragmentViewModel
        get() = _interactors

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                binding.verticalRecyclerView.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
                    .collectLatest {
                        _interactors.openPlayback(PrepareStreamLinkData.TV(it.model))
                    }
            }
        }
    }
    companion object {
        fun newInstance(filterCategory: String): PerChannelListFragment {
            return TVPerChannelListFragment().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}

class RadioPerChannelListFragment : PerChannelListFragment() {
    private val _interactors by lazy {
        RadioChannelFragmentViewModel(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val interactors: ChannelFragmentViewModel
        get() = _interactors

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                binding.verticalRecyclerView.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
                    .collectLatest {
                        _interactors.openPlayback(PrepareStreamLinkData.Radio(it.model))
                    }
            }
        }
    }
    companion object {
        fun newInstance(filterCategory: String): PerChannelListFragment {
            return RadioPerChannelListFragment().apply {
                arguments = bundleOf(
                    EXTRA_TV_CHANNEL_CATEGORY to filterCategory
                )
            }
        }
    }
}
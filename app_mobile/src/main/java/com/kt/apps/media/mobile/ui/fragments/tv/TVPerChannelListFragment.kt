package com.kt.apps.media.mobile.ui.fragments.tv

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.repeatLaunchsOnLifeCycle
import com.kt.apps.media.mobile.viewmodels.ChannelFragmentInteractors
import com.kt.apps.media.mobile.viewmodels.RadioChannelFragmentInteractors
import com.kt.apps.media.mobile.viewmodels.TVChannelFragmentInteractors
import com.kt.apps.media.mobile.viewmodels.features.loadLinkStreamChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull

class TVPerChannelListFragment : PerChannelListFragment() {
    private val _interactors by lazy {
        TVChannelFragmentInteractors(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val interactors: ChannelFragmentInteractors
        get() = _interactors

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchsOnLifeCycle(
            Lifecycle.State.STARTED, listOf {
                binding.verticalRecyclerView.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
                    .collectLatest {
                        _interactors.loadLinkStreamChannel(it)
                    }
            })
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
        RadioChannelFragmentInteractors(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope.coroutineContext)
    }
    override val interactors: ChannelFragmentInteractors
        get() = _interactors

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchsOnLifeCycle(
            Lifecycle.State.STARTED, listOf {
                binding.verticalRecyclerView.childClicks()
                    .mapNotNull { it as? ChannelElement.TVChannelElement }
                    .collectLatest {
                        _interactors.loadLinkStreamChannel(it)
                    }
            })
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
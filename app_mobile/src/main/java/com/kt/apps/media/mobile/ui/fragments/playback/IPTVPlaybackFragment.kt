package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.fillParent
import com.kt.apps.media.mobile.utils.matchParentWidth
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.safeLet
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.IPTVPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadIPTVJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class IPTVPlaybackFragment : ChannelPlaybackFragment() {

    private val _playbackViewModel by lazy {
        IPTVPlaybackInteractor(
            ViewModelProvider(requireActivity(), factory),
            viewLifecycleOwner.lifecycleScope
        )
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel


    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
    }

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        val extensionsChannel = arguments?.get(EXTRA_TV_CHANNEL) as? ExtensionsChannel
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                merge(
                    extensionsChannel?.let { flowOf(it) } ?: emptyFlow(),
                    channelListRecyclerView?.childClicks()?.mapNotNull {
                        (it as? ChannelElement.ExtensionChannelElement)?.model
                    } ?: emptyFlow()
                ).collectLatest {
                    isShowChannelList.emit(false)
                    _playbackViewModel.loadIPTVJob(it)
                }
            }

            launch {
                (((arguments?.get(EXTRA_EXTENSION_ID) as? String)?.let { flowOf(it) }) ?: emptyFlow())
                    .collectLatest {
                        _playbackViewModel.loadChannelConfig(it)
                    }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            launch {
                _playbackViewModel.relatedItems
                    .collectLatest {
                        binding.channelList?.reloadAllData(it)
                    }
            }
        }
    }


//    override fun showChannelListLayout(): ConstraintSet? {
//        return safeLet(binding.exoPlayer, binding.channelList) { exoplayer, list ->
//            ConstraintSet().apply {
//                clone(this)
//                clear(list.id)
//                fillParent(list.id)
//                setMargin(list.id, ConstraintSet.TOP, (40).dpToPx())
//            }
//        }
//    }

    companion object {
        const val screenName = "IPTVPlaybackFragment"
        private const val EXTRA_EXTENSION_ID = "extra:extension_id"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun newInstance(
            tvChannel: ExtensionsChannel,
            extension: String
        ) = IPTVPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_EXTENSION_ID to extension
            )
        }
    }
}
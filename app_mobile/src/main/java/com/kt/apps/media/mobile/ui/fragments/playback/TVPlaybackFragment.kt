package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.childClicks
import com.kt.apps.media.mobile.utils.*
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.RadioPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.TVPlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.features.loadLinkStreamChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class TVPlaybackFragment private constructor(): BasePlaybackFragment() {
    private val _playbackInteractor by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackInteractor


    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)

        val itemToPlay = arguments?.get(EXTRA_TV_CHANNEL) as? TVChannel

        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf ({
            channelListRecyclerView?.childClicks()
                ?.mapNotNull { it as? ChannelElement.TVChannelElement }
                ?.collectLatest {
                    _playbackInteractor.loadLinkStreamChannel(it)
                }
        }, {
            _playbackInteractor.channelElementList.collectLatest {
                channelListRecyclerView?.reloadAllData(it)
            }
        }))

        itemToPlay?.run {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    _playbackInteractor.loadLinkStreamChannel(ChannelElement.TVChannelElement(itemToPlay))
                }
            }
        }

    }

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.INVISIBLE)
                matchParentWidth(list.id)
                matchParentWidth(minimal.id)
                matchParentWidth(exoplayer.id)
                constrainHeight(minimal.id, ConstraintSet.WRAP_CONTENT)
                connect(exoplayer.id, ConstraintSet.BOTTOM, minimal.id, ConstraintSet.TOP)
                alignParent(minimal.id, ConstraintSet.BOTTOM)
                alignParent(exoplayer.id, ConstraintSet.TOP)
            }
        }
    }
    companion object {
        const val screenName: String = "TVPlaybackFragment"
        const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun newInstance(
            tvChannel: TVChannel
        ) = TVPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
            )
        }
    }
}

class RadioPlaybackFragment private constructor(): BasePlaybackFragment() {
    private val _playbackInteractor by lazy {
        RadioPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackInteractor

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        val itemToPlay = arguments?.get(TVPlaybackFragment.EXTRA_TV_CHANNEL) as? TVChannel

        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf ({
            merge(
                itemToPlay?.let { flowOf(ChannelElement.TVChannelElement(it)) } ?: emptyFlow(),
                channelListRecyclerView?.childClicks()
                    ?.mapNotNull { it as? ChannelElement.TVChannelElement }
                    ?: emptyFlow()
            )
                .collectLatest {
                    _playbackInteractor.loadLinkStreamChannel(it)
                }
        }, {
            _playbackInteractor.radioChannelList.collectLatest {
                channelListRecyclerView?.reloadAllData(it)
            }
        }))
    }

    override suspend fun preparePlayView(data: PrepareStreamLinkData) {
        super.preparePlayView(data)
        (data as? PrepareStreamLinkData.Radio)?.apply {
            loadArtwork(this.data)
        }
    }

    override suspend fun playVideo(data: StreamLinkData) {
        super.playVideo(data)
//        (data as? StreamLinkData.TVStreamLinkData)?.apply {
//            loadArtwork(this.data.channel)
//        }
    }
    private fun loadArtwork(data: TVChannel) {
        context?.run {
            binding.exoPlayer.useArtwork = true
            binding.exoPlayer.defaultArtwork = data.loadImgDrawable(this)
        }
    }

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.INVISIBLE)
                matchParentWidth(list.id)
                matchParentWidth(minimal.id)
                matchParentWidth(exoplayer.id)
                constrainHeight(minimal.id, ConstraintSet.WRAP_CONTENT)
                connect(exoplayer.id, ConstraintSet.BOTTOM, minimal.id, ConstraintSet.TOP)
                alignParent(minimal.id, ConstraintSet.BOTTOM)
                alignParent(exoplayer.id, ConstraintSet.TOP)
            }
        }
    }
    companion object {
        const val screenName: String = "TVPlaybackFragment"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun newInstance(
            tvChannel: TVChannel
        ) = RadioPlaybackFragment().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
            )
        }
    }
}



open class SimpleTransitionListener: MotionLayout.TransitionListener {
    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
        Log.d(TAG, "onTransitionStarted: $startId $endId")
    }

    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {
        Log.d(TAG, "onTransitionChange: $startId $endId")
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        Log.d(TAG, "onTransitionCompleted: $currentId")
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {
        Log.d(TAG, "onTransitionTrigger: $triggerId $positive $progress")
    }



}
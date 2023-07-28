package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class TVPlaybackFragment : BasePlaybackFragment() {
    private val _playbackInteractor by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackInteractor


    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
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
    }

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.GONE)
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
        val screenName: String = "TVPlaybackFragment"
    }
}

class RadioPlaybackFragment: BasePlaybackFragment() {
    private val _playbackInteractor by lazy {
        RadioPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackInteractor

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf ({
            channelListRecyclerView?.childClicks()
                ?.mapNotNull { it as? ChannelElement.TVChannelElement }
                ?.collectLatest {
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
        (data as? StreamLinkData.TVStreamLinkData)?.apply {
            loadArtwork(this.data.channel)
        }
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

                setVisibility(list.id, View.GONE)
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
        val screenName: String = "TVPlaybackFragment"
    }
}

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        BasePlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.GONE)
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
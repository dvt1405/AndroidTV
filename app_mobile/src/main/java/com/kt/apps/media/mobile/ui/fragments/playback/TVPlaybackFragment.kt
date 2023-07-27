package com.kt.apps.media.mobile.ui.fragments.playback

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.utils.OnSwipeTouchListener
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.TVPlaybackInteractor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class TVPlaybackFragment : BasePlaybackFragment() {
    private val _playbackViewModel by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel


    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

    }


    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            _playbackViewModel.channelElementList.collectLatest {
                channelListRecyclerView?.reloadAllData(it)
            }
        }

    }



    override fun onDestroy() {
        super.onDestroy()
    }

}

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        BasePlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

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
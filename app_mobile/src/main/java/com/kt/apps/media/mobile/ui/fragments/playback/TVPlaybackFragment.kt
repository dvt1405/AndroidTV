package com.kt.apps.media.mobile.ui.fragments.playback

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.AbsListView.OnScrollListener
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelAdapter
import com.kt.apps.media.mobile.ui.view.ChannelListView
import com.kt.apps.media.mobile.utils.OnSwipeTouchListener
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.TVPlaybackInteractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs


interface IDispatchTouchListener {
    fun onDispatchTouchEvent(view: View?, mv: MotionEvent)
}
class TVPlaybackFragment : BasePlaybackFragment(), IDispatchTouchListener {
    private val _playbackViewModel by lazy {
        TVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

    private val channelListRecyclerView: ChannelListView? by lazy {
        binding.channelList
    }
    private val gestureDetector by lazy {
        object: OnSwipeTouchListener(requireContext()) {
            override fun onSwipeTop() {
                showChannelList()
            }

            override fun onSwipeBottom() {
                changeFullScreenLayout()
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

        binding.exoPlayer.setControllerVisibilityListener(StyledPlayerView.ControllerVisibilityListener { visibility ->
            when(visibility) {
                View.GONE, View.INVISIBLE -> {
//                    if (binding.motionLayout?.currentState != R.id.show_list) {
//                        channelListRecyclerView?.fadeOut {  }
//                    }
                }
                else -> channelListRecyclerView?.fadeIn {  }
            }
        })


        channelListRecyclerView?.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                Log.d(TAG, "onScrolled: $dx $dy")
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

//                }
            }
        })
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

    override fun onDispatchTouchEvent(view: View?, mv: MotionEvent) {
        gestureDetector.onTouch(this.requireView(), mv)
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
package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.utils.CustomTransition
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.fillParent
import com.kt.apps.media.mobile.utils.safeLet
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.properties.Delegates

interface ComplexLayoutHandler {
    var onPlaybackStateChange: (PlaybackState) -> Unit
    fun onStartLoading()
    fun onLoadedVideoSuccess(videoSize: VideoSize)
    fun onOpenFullScreen()

    fun forceFullScreen() { }

    fun onCloseMinimal()
    fun onTouchEvent(ev: MotionEvent) { }
    fun onBackEvent() : Boolean { return false }
    fun onReset(isPlaying: Boolean) { }
    fun onPlayPause(isPause: Boolean) { }

    fun confirmState(state: PlaybackState) { }
}

class PortraitLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler {

    private val swipeThreshold = 100
    private val velocitySwipeThreshold = 100

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    private val guideline by lazy {
        weakActivity.get()?.binding?.guidelineComplex
    }

    override var onPlaybackStateChange: (PlaybackState) -> Unit = { }
    private var state: PlaybackState by Delegates.observable(PlaybackState.Invisible) { _, oldValue, newValue ->
        if (oldValue !== newValue) {
            onPlaybackStateChange(newValue)
        }
    }
    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffY = e2.y - e1.y
                if (abs(diffY) > swipeThreshold && abs(velocityY) > velocitySwipeThreshold) {
                    if (diffY > 0) {
                        onSwipeBottom(e1, e2)
                    } else {
                        onSwipeUp(e1, e2)
                    }
                }
                return false
            }
        })
    }

    override fun confirmState(state: PlaybackState) {
        if (state != this.state) {
            when(state) {
                PlaybackState.Invisible -> transitionIDLE()
                PlaybackState.Fullscreen -> transitionFullscreen()
                PlaybackState.Minimal -> transitionMinimal()
            }
        }
    }
    override fun onTouchEvent(ev: MotionEvent) {
        gestureDetector.onTouchEvent(ev)
    }

    override fun onOpenFullScreen() {
        if (state != PlaybackState.Fullscreen) {
            transitionFullscreen()
        } else {
            transitionMinimal()
        }

    }

    override fun forceFullScreen() {
        guideline?.run {
            setGuidelinePercent(1f)
            this@PortraitLayoutHandler.state = PlaybackState.Fullscreen
        }
    }

    override fun onCloseMinimal() {
        transitionIDLE()
    }

    override fun onBackEvent() : Boolean {
        if (state == PlaybackState.Fullscreen) {
            onOpenFullScreen()
            return true
        }
        return false
    }

    override fun onStartLoading() {
        if (state != PlaybackState.Minimal) {
            transitionMinimal()
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) { }


    override fun onReset(isPlaying: Boolean) {
        if (isPlaying) {
            transitionMinimal()
        } else {
            transitionIDLE()
        }
    }

    fun onSwipeUp(e1: MotionEvent, e2: MotionEvent) {
        val hitRect = Rect()
        val location = intArrayOf(0, 0)
        weakActivity.get()?.binding?.fragmentContainerChannels?.run {
            getHitRect(hitRect)
            getLocationOnScreen(location)
            if (hitRect.contains(e1.x.toInt(), e1.y.toInt())) {
                Log.d(TAG, "onSwipeUp: ")
                if (state == PlaybackState.Fullscreen) {
                    transitionMinimal()
                }
            }
        }
    }
    fun onSwipeBottom(e1: MotionEvent, e2: MotionEvent) {
        val hitRect = Rect()
        val location = intArrayOf(0, 0)
        fragmentContainerPlayback?.getHitRect(hitRect)
        fragmentContainerPlayback?.getLocationOnScreen(location)

        if (hitRect.contains(e1.x.toInt(), e1.y.toInt())) {
            Log.d(TAG, "onSwipeBottom: ")
            transitionFullscreen()
        }
    }

    private fun transitionFullscreen() {
        guideline?.run {
            setGuidelinePercent(0.6f)
            this@PortraitLayoutHandler.state = PlaybackState.Fullscreen
        }
    }

    private fun transitionMinimal() {
        guideline?.run {
            setGuidelinePercent(0.3f)
            this@PortraitLayoutHandler.state = PlaybackState.Minimal
        }
    }

    private fun transitionIDLE() {
        guideline?.run {
            setGuidelinePercent(0f)
            this@PortraitLayoutHandler.state = PlaybackState.Invisible
        }
    }
}
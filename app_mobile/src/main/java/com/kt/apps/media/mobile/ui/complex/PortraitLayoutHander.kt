package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackState
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

    private val surfaceView: ConstraintLayout? by lazy {
        weakActivity.get()?.binding?.surfaceView as? ConstraintLayout
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
                PlaybackState.PIP -> transitionPIP()
                else -> transitionToState(state)
            }
        }
    }
    override fun onTouchEvent(ev: MotionEvent) {
        gestureDetector.onTouchEvent(ev)
    }

    override fun onOpenFullScreen() {
        if (state != PlaybackState.Fullscreen) {
            transitionToState(PlaybackState.Fullscreen)
        } else {
            transitionToState(PlaybackState.Minimal)
        }

    }

    override fun forceFullScreen() {
        guideline?.run {
            setGuidelinePercent(1f)
            this@PortraitLayoutHandler.state = PlaybackState.Fullscreen
        }
    }

    override fun onCloseMinimal() {
        transitionToState(PlaybackState.Invisible)
    }

    override fun onBackEvent() : Boolean {
        if (state == PlaybackState.Fullscreen) {
            onOpenFullScreen()
            return true
        }
        if (state == PlaybackState.Minimal) {
            transitionToState(PlaybackState.Invisible)
            return true
        }
        return false
    }

    override fun onStartLoading() {
        if (state != PlaybackState.Minimal) {
            transitionToState(PlaybackState.Minimal)
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) { }


    override fun onReset(isPlaying: Boolean) {
        if (isPlaying) {
            transitionToState(PlaybackState.Minimal)
        } else {
            transitionToState(PlaybackState.Invisible)
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
                    transitionToState(PlaybackState.Minimal)
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
            transitionToState(PlaybackState.Fullscreen)
        }
    }

    private fun transitionPIP() {

    }

    private fun transitionToState(state: PlaybackState) {
        this.state = state
        safeLet(surfaceView, guideline) { root, guideline ->
            guideline.setGuidelinePercent(when(state) {
                PlaybackState.Fullscreen -> 0.6f
                PlaybackState.Invisible, PlaybackState.PIP -> 0f
                PlaybackState.Minimal -> 0.3f
            })
            TransitionManager.beginDelayedTransition(root, AutoTransition())
        }
    }
}
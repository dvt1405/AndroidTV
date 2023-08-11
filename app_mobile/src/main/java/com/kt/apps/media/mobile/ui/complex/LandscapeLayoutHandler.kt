package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.ChangeScroll
import androidx.transition.ChangeTransform
import androidx.transition.Explode
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.utils.CustomTransition
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.fillParent
import com.kt.apps.media.mobile.utils.safeLet
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

class LandscapeLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {
    sealed class State {
        object IDLE: State()
        object MINIMAL: State()
        object FULLSCREEN: State()
    }

//    private var state: State = State.IDLE
    private var state: State by Delegates.observable(State.IDLE) { property, oldValue, newValue ->
        if (oldValue !== newValue) {
            onPlaybackStateChange(when(newValue) {
                State.IDLE -> PlaybackState.Invisible
                State.MINIMAL -> PlaybackState.Minimal
                State.FULLSCREEN -> PlaybackState.Fullscreen
            })
        }
    }

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    private val surfaceView: ConstraintLayout? by lazy {
        weakActivity.get()?.binding?.surfaceView as? ConstraintLayout
    }

    override var onPlaybackStateChange: (PlaybackState) -> Unit = { }


    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {

        }).apply {
            this.setOnDoubleTapListener(object: GestureDetector.OnDoubleTapListener {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "onDoubleTap: ")
                    this@LandscapeLayoutHandler.onDoubleTap(e)
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    return true
                }
            })
        }
    }
    override fun onStartLoading() {
        if (state == State.FULLSCREEN) {
            return
        }
        Log.d(TAG, "onStartLoading: $state")
        transitionFullscreen()
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) { }

    override fun onOpenFullScreen() {
        if (state != State.FULLSCREEN) {
            transitionFullscreen()
        } else {
            transitionMinimal()
        }
    }

    override fun forceFullScreen() {
        transitionFullscreen()
    }

    override fun onCloseMinimal() {
        transitionIDLE()
    }

    override fun onBackEvent(): Boolean {
        if (state == State.FULLSCREEN) {
            transitionMinimal()
            return true
        }
        return false
    }

    override fun onReset(isPlaying: Boolean) {
        if (isPlaying) {
            transitionFullscreen()
        } else {
            transitionIDLE()
        }
    }

    override fun onTouchEvent(ev: MotionEvent) {
        gestureDetector.onTouchEvent(ev)
    }

    private fun onDoubleTap(ev: MotionEvent) {
        val hitRect = Rect()
        if (fragmentContainerPlayback?.visibility == View.VISIBLE) else return
        fragmentContainerPlayback?.getHitRect(hitRect)
        if (hitRect.contains(ev.x.toInt(), ev.y.toInt())) {
            transitionFullscreen()
        }
    }

    private fun transitionFullscreen() {
        state = State.FULLSCREEN
        safeLet(surfaceView, fragmentContainerPlayback) {
                surfaceView, playback ->
            val set = ConstraintSet().apply {
                clone(surfaceView)
                clear(playback.id)
                fillParent(playback.id)
            }

            TransitionManager.beginDelayedTransition(
                surfaceView,
                AutoTransition()
            )
            set.applyTo(surfaceView)
        }
    }

    private fun transitionMinimal() {
        safeLet(surfaceView, fragmentContainerPlayback) {
                surfaceView, playback ->
            val set = ConstraintSet().apply {
                clone(surfaceView)
                clear(playback.id)
                alignParent(playback.id, ConstraintSet.BOTTOM)
                alignParent(playback.id, ConstraintSet.END)
                constrainPercentWidth(playback.id, 0.4f)
                constrainPercentHeight(playback.id, 0.5f)
            }

            TransitionManager.beginDelayedTransition(surfaceView, CustomTransition().apply {
                addListener(object: TransitionCallback() {
                    override fun onTransitionStart(transition: Transition) {
                        super.onTransitionStart(transition)
                        this@LandscapeLayoutHandler.state = State.MINIMAL
                    }
                })
            })
            set.applyTo(surfaceView)
        }
    }


    private fun transitionIDLE() {
        safeLet(surfaceView, fragmentContainerPlayback) {
                surfaceView, playback ->
            val set = ConstraintSet().apply {
                clone(surfaceView)
                clear(playback.id)
                alignParent(playback.id, ConstraintSet.BOTTOM)
                alignParent(playback.id, ConstraintSet.END)
            }
            TransitionManager.beginDelayedTransition(surfaceView, CustomTransition().apply {
                addListener(object: TransitionCallback() {
                    override fun onTransitionStart(transition: Transition) {
                        super.onTransitionStart(transition)
                        this@LandscapeLayoutHandler.state = State.IDLE
                    }
                })
            })
            set.applyTo(surfaceView)
        }
    }

}
open class TransitionCallback: Transition.TransitionListener {
    override fun onTransitionStart(transition: Transition) {
        Log.d(TAG, "onTransitionStart: $transition")
    }

    override fun onTransitionEnd(transition: Transition) {
        Log.d(TAG, "onTransitionEnd: $transition")
    }

    override fun onTransitionCancel(transition: Transition) {
        Log.d(TAG, "onTransitionCancel: $transition")
    }

    override fun onTransitionPause(transition: Transition) {
        Log.d(TAG, "onTransitionPause: $transition")
    }

    override fun onTransitionResume(transition: Transition) {
        Log.d(TAG, "onTransitionResume: $transition")
    }

}
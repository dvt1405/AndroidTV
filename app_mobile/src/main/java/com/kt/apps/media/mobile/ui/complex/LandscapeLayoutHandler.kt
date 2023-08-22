package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
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
import kotlin.properties.Delegates

class LandscapeLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {

    private var state: PlaybackState by Delegates.observable(PlaybackState.Invisible) { _, oldValue, newValue ->
        if (oldValue !== newValue) {
            onPlaybackStateChange(newValue)
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

                    this@LandscapeLayoutHandler.onDoubleTap(e)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "onDoubleTap: ")
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    return true
                }
            })
        }
    }

    override fun confirmState(state: PlaybackState) {
        if (state != this.state) {
            when(state) {
                PlaybackState.Fullscreen -> transitionFullscreen()
                PlaybackState.Minimal -> transitionMinimal()
                PlaybackState.Invisible -> transitionIDLE()
            }
        }
    }
    override fun onStartLoading() {
        if (state == PlaybackState.Fullscreen) {
            return
        }
        Log.d(TAG, "onStartLoading: $state")
        transitionFullscreen()
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) { }

    override fun onOpenFullScreen() {
        if (state != PlaybackState.Fullscreen) {
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
        if (state == PlaybackState.Fullscreen) {
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
        if (state == PlaybackState.Minimal) {
            gestureDetector.onTouchEvent(ev)
        }
    }

    private fun onDoubleTap(ev: MotionEvent) {
        val hitRect = Rect()
        if (fragmentContainerPlayback?.visibility == View.VISIBLE) else return
        fragmentContainerPlayback?.getHitRect(hitRect)
        if (hitRect.contains(ev.x.toInt(), ev.y.toInt())) {
            if (state == PlaybackState.Minimal) {
                transitionFullscreen()
            }
        }
    }

    private fun transitionFullscreen() {
        state = PlaybackState.Fullscreen
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
            playback.setBackgroundColor(Color.BLACK)
            playback.clipToOutline = false
        }
    }

    private fun transitionMinimal() {
        state = PlaybackState.Minimal
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

            TransitionManager.beginDelayedTransition(surfaceView, CustomTransition())
            set.applyTo(surfaceView)
            playback.background = AppCompatResources.getDrawable(playback.context, R.drawable.playback_minimal_bg)
            playback.clipToOutline = true

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
                        this@LandscapeLayoutHandler.state = PlaybackState.Invisible
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
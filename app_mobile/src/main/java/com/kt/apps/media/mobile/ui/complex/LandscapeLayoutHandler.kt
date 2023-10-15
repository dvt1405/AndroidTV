package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.models.PlaybackState
import com.kt.apps.media.mobile.utils.CustomTransition
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.fillParent
import com.kt.apps.media.mobile.utils.safeLet
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class LandscapeLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {

    private var state: PlaybackState by Delegates.observable(PlaybackState.Invisible) { _, oldValue, newValue ->
        if (oldValue !== newValue) {
            Log.d(TAG, "onPlaybackStateChange: $oldValue -> $newValue")
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

                    this@LandscapeLayoutHandler.onSingleTap(e)
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
                PlaybackState.PIP -> transitionPIP()
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

    override fun changeToMinimal() {
        if (state != PlaybackState.Invisible && state != PlaybackState.Minimal) {
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
        if (state == PlaybackState.Minimal) {
            transitionIDLE()
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

    private fun onSingleTap(ev: MotionEvent) {
        val hitRect = Rect()
        if (fragmentContainerPlayback?.visibility == View.VISIBLE) else return
        fragmentContainerPlayback?.getHitRect(hitRect)
        if (hitRect.contains(ev.x.toInt(), ev.y.toInt())) {
            if (state == PlaybackState.Minimal) {
                transitionFullscreen()
            }
        }
    }

    private fun transitionPIP() {

    }

    private fun transitionFullscreen() {
        state = PlaybackState.Fullscreen
        Log.d(TAG, "transitionFullscreen: ${Log.getStackTraceString(Throwable())}")
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
        Log.d(TAG, "transitionMinimal: ${Log.getStackTraceString(Throwable())}")
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
            playback.outlineProvider = RoundedCornersOutlineProvider((12).dpToPx().toFloat(), (12).dpToPx().toFloat(), (12).dpToPx().toFloat(), 0F, 0F)
            playback.clipToOutline = true

        }
    }


    private fun transitionIDLE() {
        state = PlaybackState.Invisible
        Log.d(TAG, "transitionIDLE: ${Log.getStackTraceString(Throwable())}")
        safeLet(surfaceView, fragmentContainerPlayback) {
                surfaceView, playback ->
            val set = ConstraintSet().apply {
                clone(surfaceView)
                clear(playback.id)
                alignParent(playback.id, ConstraintSet.BOTTOM)
                alignParent(playback.id, ConstraintSet.END)
            }
            TransitionManager.beginDelayedTransition(surfaceView, Fade(Fade.IN))
            set.applyTo(surfaceView)
        }
    }

}

class RoundedCornersOutlineProvider(
    val radius: Float? = null,
    val topLeft: Float? = null,
    val topRight: Float? = null,
    val bottomLeft: Float? = null,
    val bottomRight: Float? = null,
) : ViewOutlineProvider() {

    private val topCorners = topLeft != null && topLeft == topRight
    private val rightCorners = topRight != null && topRight == bottomRight
    private val bottomCorners = bottomLeft != null && bottomLeft == bottomRight
    private val leftCorners = topLeft != null && topLeft == bottomLeft
    private val topLeftCorner = topLeft != null
    private val topRightCorner = topRight != null
    private val bottomRightCorner = bottomRight != null
    private val bottomLeftCorner = bottomLeft != null

    override fun getOutline(view: View, outline: Outline) {
        val left = 0
        val top = 0
        val right = view.width
        val bottom = view.height

        if (radius != null) {
            val cornerRadius = radius //.typedValue(resources).toFloat()
            outline.setRoundRect(left, top, right, bottom, cornerRadius)
        } else {
            val cornerRadius = topLeft ?: topRight ?: bottomLeft ?: bottomRight ?: 0F

            when {
                topCorners -> outline.setRoundRect(left, top, right, bottom + cornerRadius.toInt(), cornerRadius)
                bottomCorners -> outline.setRoundRect(left, top - cornerRadius.toInt(), right, bottom, cornerRadius)
                leftCorners -> outline.setRoundRect(left, top, right + cornerRadius.toInt(), bottom, cornerRadius)
                rightCorners -> outline.setRoundRect(left - cornerRadius.toInt(), top, right, bottom, cornerRadius)
                topLeftCorner -> outline.setRoundRect(
                    left, top, right + cornerRadius.toInt(), bottom + cornerRadius.toInt(), cornerRadius
                )
                bottomLeftCorner -> outline.setRoundRect(
                    left, top - cornerRadius.toInt(), right + cornerRadius.toInt(), bottom, cornerRadius
                )
                topRightCorner -> outline.setRoundRect(
                    left - cornerRadius.toInt(), top, right, bottom + cornerRadius.toInt(), cornerRadius
                )
                bottomRightCorner -> outline.setRoundRect(
                    left - cornerRadius.toInt(), top - cornerRadius.toInt(), right, bottom, cornerRadius
                )
            }
        }
    }
}
package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import java.lang.ref.WeakReference
import kotlin.math.abs


interface ComplexLayoutHandler {
    val motionLayout: MotionLayout?

    fun onStartLoading()
    fun onLoadedVideoSuccess(videoSize: VideoSize)
    fun onOpenFullScreen()
    fun onTouchEvent(ev: MotionEvent) { }
    fun onBackEvent() : Boolean { return false }
    fun onReset(isPlaying: Boolean) { }
    fun onPlayPause(isPause: Boolean) { }
}

class PortraitLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler {
    sealed class State {
        object IDLE: State()
        object LOADING: State()
        data class SUCCESS(val videoSize: VideoSize): State()
        object FULLSCREEN: State()
    }

    private val swipeThreshold = 100
    private val velocitySwipeThreshold = 100

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    override val motionLayout: MotionLayout?
        get() = weakActivity.get()?.binding?.complexMotionLayout

    private var state: State = State.IDLE
    private var cachedVideoSize: VideoSize? = null
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
                    }
                }
                return false
            }
        })
    }

    override fun onTouchEvent(ev: MotionEvent) {
        gestureDetector.onTouchEvent(ev)
    }

    override fun onOpenFullScreen() {
        if (state != State.FULLSCREEN) {
            motionLayout?.transitionToState(R.id.fullscreen)
            state = State.FULLSCREEN
            return
        }
        if (state == State.FULLSCREEN) {
            cachedVideoSize?.let {
                motionLayout?.transitionToState(R.id.end)
                state = State.SUCCESS(it)
            } ?: run {
                motionLayout?.transitionToState(R.id.end)
                state = State.IDLE
            }
        }

    }

    override fun onBackEvent() : Boolean {
        if (state == State.FULLSCREEN) {
            onOpenFullScreen()
            return true
        }
        return false
    }

    override fun onStartLoading() {
        if (state == State.IDLE) {
            motionLayout?.transitionToState(R.id.end)
            state = State.LOADING
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) {
        this.state = State.SUCCESS(videoSize)
        calculateCurrentSize(videoSize)
    }


    override fun onReset(isPlaying: Boolean) {
        state = if (isPlaying) {
            motionLayout?.getConstraintSet(R.id.end)?.let {
                it.setGuidelinePercent(R.id.guideline_complex, 0.3F)
                motionLayout?.transitionToState(R.id.end)
            }
            State.LOADING
        } else {
            motionLayout?.transitionToState(R.id.start)
            State.IDLE
        }
    }

    private fun calculateCurrentSize(size: VideoSize) {
        val motionLayout = motionLayout?: return
        val wpx = motionLayout.resources.displayMetrics.widthPixels
        val hpx = motionLayout.resources.displayMetrics.heightPixels
        if (size.width == 0 || size.height == 0) {
            return
        }
        val newHeight = wpx  / (size.width * 1.0 / size.height)
        val percentage: Float = (newHeight * 1.0 / hpx).toFloat()

        motionLayout.getConstraintSet(R.id.end)?.let {
            it.setGuidelinePercent(R.id.guideline_complex, percentage)
            motionLayout.transitionToState(R.id.end)
        }
    }

    fun onSwipeBottom(e1: MotionEvent, e2: MotionEvent) {
        val hitRect = Rect()
        val location = intArrayOf(0, 0)
        fragmentContainerPlayback?.getHitRect(hitRect)
        fragmentContainerPlayback?.getLocationOnScreen(location)

//        hitRect.offset(location[0], location[1])
        if (hitRect.contains(e1.x.toInt(), e1.y.toInt())) {
            Log.d(TAG, "onSwipeBottom: ")
            motionLayout?.transitionToState(R.id.fullscreen)
        }
    }


}
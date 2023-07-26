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
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.utils.hitRectOnScreen
import java.lang.ref.WeakReference

class LandscapeLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {
    sealed class State {
        object IDLE: State()
        object MINIMAL: State()
        object FULLSCREEN: State()
    }

    private val state: State
        get() {
            return motionLayout?.currentState?.let {
                when(it) {
                    R.id.start -> State.IDLE
                    R.id.end -> State.MINIMAL
                    R.id.fullscreen -> State.FULLSCREEN
                    else -> State.IDLE
                }
            } ?: State.IDLE
        }
    private var cachedVideoSize: VideoSize? = null
    private var videoIsLoading: Boolean = false

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    override val motionLayout: MotionLayout?
        get() = weakActivity.get()?.binding?.complexMotionLayout

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
    init {
        motionLayout?.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
                Log.d(TAG, "onTransitionStarted: ${motionLayout?.startState} ${motionLayout?.endState} ${motionLayout?.currentState}")
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
                Log.d(TAG, "onTransitionCompleted: $currentId ${R.id.fullscreen}")
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
                Log.d(TAG, "onTransitionTrigger: $triggerId")
            }

        })
    }
    override fun onStartLoading() {
        Log.d(TAG, "onStartLoading: $state")
        if (state != State.FULLSCREEN || state != State.MINIMAL) {

            motionLayout?.transitionToState(R.id.fullscreen)
        }
        videoIsLoading = true
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) {
        cachedVideoSize = videoSize
        val isFullScreenState = motionLayout?.currentState == R.id.fullscreen
        if (state != State.FULLSCREEN || !isFullScreenState) {
            motionLayout?.setTransitionDuration(250)
            transitionToState(R.id.fullscreen)
        }
        videoIsLoading = false
    }

    override fun onOpenFullScreen() {
        if (state != State.FULLSCREEN) {
            transitionToState(R.id.fullscreen)
            State.FULLSCREEN
        } else {
            transitionToState(R.id.end)
            State.MINIMAL
        }
    }

    override fun onCloseMinimal() {
        transitionToState(R.id.start)
    }

    override fun onBackEvent(): Boolean {
        if (state == State.FULLSCREEN) {
            transitionToState(R.id.end)
            return true
        }
        return false
    }

    override fun onReset(isPlaying: Boolean) {
        if (isPlaying) {
            transitionToState(R.id.fullscreen)
            State.FULLSCREEN
        } else {
            transitionToState(R.id.start)
            State.IDLE
        }
    }

    override fun onPlayPause(isPause: Boolean) {
        super.onPlayPause(isPause)
        if (videoIsLoading) return
        if (isPause) {
            if (state == State.FULLSCREEN) {
                transitionToState(R.id.end)
            }
        } else {
            if (state != State.FULLSCREEN) {
                transitionToState(R.id.fullscreen)
            }
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
            transitionToState(R.id.fullscreen)
        }
    }

    private fun transitionToState(id: Int) {
        onPlaybackStateChange(when(id) {
            R.id.fullscreen -> PlaybackState.Fullscreen
            R.id.end -> PlaybackState.Minimal
            R.id.start -> PlaybackState.Invisible
            else -> PlaybackState.Invisible
        })
        motionLayout?.transitionToState(id)
    }

}
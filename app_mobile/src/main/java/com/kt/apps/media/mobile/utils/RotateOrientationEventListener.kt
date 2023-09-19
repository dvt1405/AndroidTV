package com.kt.apps.media.mobile.utils

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import com.kt.apps.core.utils.TAG
import kotlin.math.abs


abstract  class RotateOrientationEventListener(context: Context?) : OrientationEventListener(context) {

    private var lastOrientation: Int? = null
    val currentOrientation
        get() = lastOrientation

    private var lastOrientationValue: Int? = null
    override fun onOrientationChanged(orientation: Int) {
        Log.d(TAG, "onOrientationChanged: $orientation $lastOrientation")
        if (lastOrientationValue == null) {
            lastOrientationValue = orientation
        }
        if (abs(lastOrientationValue!! - orientation) < 20) {
            return
        }

        val defaultPortrait = 0
        val upsideDownPortrait = 180
        val rightLandscape = 90
        val leftLandscape = 270
        val curOrientation = when {
            isWithinOrientationRange(orientation, defaultPortrait) -> ORIENTATION_PORTRAIT
            isWithinOrientationRange(orientation, leftLandscape) -> ORIENTATION_LANDSCAPE
            isWithinOrientationRange(orientation, upsideDownPortrait) -> ORIENTATION_PORTRAIT
            isWithinOrientationRange(orientation, rightLandscape) -> ORIENTATION_LANDSCAPE
            else -> lastOrientation ?: ORIENTATION_PORTRAIT
        }


        if (lastOrientation == null) {
            lastOrientation = curOrientation
            return
        }

        if (curOrientation != lastOrientation) {
            onChanged(lastOrientation!!, curOrientation)
            this.lastOrientation = curOrientation
            lastOrientationValue = orientation
        }
    }

    private fun isWithinOrientationRange(
        currentOrientation: Int, targetOrientation: Int, epsilon: Int = 10
    ): Boolean {
        return currentOrientation > targetOrientation - epsilon
                && currentOrientation < targetOrientation + epsilon
    }

    public abstract fun onChanged(lastOrientation: Int, orientation: Int)

    companion object {
        const val ORIENTATION_PORTRAIT = 0
        const val ORIENTATION_LANDSCAPE = 1
    }
}
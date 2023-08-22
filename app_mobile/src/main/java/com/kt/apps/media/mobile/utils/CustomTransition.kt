package com.kt.apps.media.mobile.utils

import android.view.animation.LinearInterpolator
import androidx.transition.Fade
import androidx.transition.TransitionSet

class CustomTransition: TransitionSet() {
    init {
        ordering = ORDERING_SEQUENTIAL
        interpolator = LinearInterpolator()
        duration = 250
        addTransition(Fade())
    }
}
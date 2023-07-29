package com.kt.apps.media.mobile.utils

import android.view.animation.AccelerateInterpolator
import androidx.transition.Explode
import androidx.transition.Fade
import androidx.transition.TransitionSet

class CustomTransition: TransitionSet() {
    init {
        ordering = ORDERING_SEQUENTIAL
        interpolator = AccelerateInterpolator()
        duration = 500
        addTransition(Fade(Fade.IN))
            .addTransition(Explode())
            .addTransition(Fade(Fade.OUT))
    }
}
package com.kt.apps.media.mobile.utils

import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


fun RecyclerView.fastSmoothScrollToPosition(newPosition: Int) {
    layoutManager?.startSmoothScroll((object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }
    }).apply {
        this.targetPosition = newPosition
    })
}
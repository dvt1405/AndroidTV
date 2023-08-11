package com.kt.apps.media.mobile.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.kt.apps.core.utils.dpToPx

class PaddingItemDecoration(private val edge: Edge): ItemDecoration() {
    data class Edge(val top: Int, val right: Int, val bottom: Int, val left: Int)
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(
            edge.left.dpToPx(),
            edge.top.dpToPx(),
            edge.right.dpToPx(),
            edge.bottom.dpToPx()
        )
    }
}
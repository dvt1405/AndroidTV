package com.kt.apps.media.mobile.utils

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.utils.dpToPx
import kotlin.math.max


class GridAutoFitLayoutManager(context: Context?, colWidth: Int) :
    GridLayoutManager(context, 1) {

    private var colWidth: Int = 0
    private var isColumnWidthChanged: Boolean = false
    private var lastHeight: Int = 0
    private var lastWidth: Int = 0

    init {
        setColWidth(if (colWidth <= 0) {
            (48).dpToPx()
        } else colWidth)
    }

    private fun setColWidth(newWidth: Int) {
        if (newWidth > 0 && newWidth != colWidth) {
            colWidth = newWidth
            isColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        val width = this.width
        val height = this.height
        if (colWidth > 0 && width > 0 && height > 0 && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {
            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = max(1, totalSpace / colWidth)
            setSpanCount(spanCount)
        }
        lastHeight = height
        lastWidth = width
        super.onLayoutChildren(recycler, state)
    }
}
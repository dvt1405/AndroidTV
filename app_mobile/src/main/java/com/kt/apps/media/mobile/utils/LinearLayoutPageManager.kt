package com.kt.apps.media.mobile.utils

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.utils.dpToPx
import kotlin.math.max

class LinearLayoutPagerManager(context: Context, orientation: Int, reverseLayout: Boolean, private val itemsPerPage: Int) : LinearLayoutManager(context,orientation,reverseLayout) {

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
        return super.checkLayoutParams(lp) && lp!!.width == getItemSize()
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return setProperItemSize(super.generateDefaultLayoutParams())
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return setProperItemSize(super.generateLayoutParams(lp))
    }

    private fun setProperItemSize(lp: RecyclerView.LayoutParams): RecyclerView.LayoutParams {
        val itemSize = getItemSize()
        if (orientation == HORIZONTAL) {
            lp.width = itemSize
        } else {
            lp.height = itemSize
        }
        return lp
    }

    private fun getItemSize(): Int {
        val pageSize = if (orientation == HORIZONTAL) width else height
        return Math.round(pageSize.toFloat() / itemsPerPage)
    }


}

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
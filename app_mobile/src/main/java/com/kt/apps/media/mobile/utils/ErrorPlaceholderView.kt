package com.kt.apps.media.mobile.utils

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ViewSwitcher
import com.google.android.material.textview.MaterialTextView
import com.kt.apps.media.mobile.R

class ErrorPlaceholderView(context: Context?, attrs: AttributeSet?) : ViewSwitcher(context, attrs) {
    private val errorLayoutId by lazy { View.generateViewId() }
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount <= 1) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.recycler_view_error_layout, null)
            view.id = errorLayoutId
            addView(view)
        }
    }

    fun showErrorMessage(text: String) {
        val view = if (currentView.id == errorLayoutId) currentView else nextView
        view.findViewById<MaterialTextView>(R.id.error_text_view).text = text
        showError()
    }

    fun showError() {
        if (nextView.id == errorLayoutId) {
            showNext()
        }
    }

    fun showContentView() {
        if (nextView.id != errorLayoutId) {
            showNext()
        }
    }
}
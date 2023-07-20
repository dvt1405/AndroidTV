package com.kt.apps.media.mobile.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.media.mobile.R

enum class State {
    LOADING, SUCCESS, ERROR
}

class StateLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val successView by lazy {
        findViewById<View>(R.id.success_view)
    }
    private val errorView by lazy {
        findViewById<View>(R.id.error_view)
    }
    private val loadingView by lazy {
        findViewById<View>(R.id.loading_view)
    }
    init {
        View.inflate(context, R.layout.state_loading_view, this)
    }

    fun startLoading() {
        switchToState(State.LOADING)
    }

    fun showSuccess() {
        switchToState(State.SUCCESS)
    }

    fun showError() {
        switchToState(State.ERROR)
    }

    private fun switchToState(state: State) {
        listOf(successView, errorView, loadingView).forEach {
            it.visibility = View.GONE
        }
        when(state) {
            State.LOADING -> loadingView
            State.SUCCESS -> successView
            State.ERROR -> errorView
        }.fadeIn {  }
    }
}
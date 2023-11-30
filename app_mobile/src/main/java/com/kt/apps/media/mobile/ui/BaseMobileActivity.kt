package com.kt.apps.media.mobile.ui

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.databinding.ViewDataBinding
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.media.mobile.R

abstract class BaseMobileActivity<T : ViewDataBinding> : BaseActivity<T>() {
    protected val isLandscape: Boolean
        get() = resources.getBoolean(R.bool.is_landscape)

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        super.onCreate(savedInstanceState)
    }
}
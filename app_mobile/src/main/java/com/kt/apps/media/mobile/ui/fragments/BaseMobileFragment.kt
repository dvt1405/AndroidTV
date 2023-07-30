package com.kt.apps.media.mobile.ui.fragments

import androidx.databinding.ViewDataBinding
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R

abstract class BaseMobileFragment<T : ViewDataBinding>: BaseFragment<T>() {
    protected val isLandscape: Boolean
        get() = resources.getBoolean(R.bool.is_landscape)
}
package com.kt.apps.core.base

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.DaggerDialogFragment
import javax.inject.Inject

open class BaseDialogFragment: DaggerDialogFragment() {

    override fun show(manager: FragmentManager, tag: String?) {

        dialog?.window?.run {
            setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            decorView.systemUiVisibility = FULLSCREEN_FLAGS
        }

        super.show(manager, tag)
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        this.activity?.window?.run {
            decorView.systemUiVisibility = FULLSCREEN_FLAGS
        }
    }

    companion object {
        const val FULLSCREEN_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}

abstract class BindingDialogFragment<T: ViewDataBinding> : BaseDialogFragment() {
    lateinit var binding: T
    abstract val layoutResId: Int

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun initAction(savedInstanceState: Bundle?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        initView(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAction(savedInstanceState)
    }
}
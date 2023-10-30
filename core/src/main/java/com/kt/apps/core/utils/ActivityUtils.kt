package com.kt.apps.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.pedant.SweetAlert.SweetAlertDialog
import com.kt.apps.core.R
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.logging.Logger
import java.util.*

fun Context.updateLocale(language: String = "vi"): Context {
    val config = Configuration()
    val locale = Locale(language)
    config.setLocale(locale)
    Locale.setDefault(locale)
    return createConfigurationContext(config)
}

fun Fragment.hideKeyboard() {
    requireActivity().hideKeyboard()
}
fun Activity.hideKeyboard() {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(it.windowToken, 0)
    }

}

fun Fragment.showSuccessDialog(
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = true

) {
    requireActivity().showSweetDialog(SweetAlertDialog.SUCCESS_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
}

fun Fragment.showErrorDialog(
    onSuccessListener: (() -> Unit)? = null,
    content: String? = null,
    titleText: String? = null,
    confirmText: String? = "OK",
    delayMillis: Int? = 1900,
    cancellable: Boolean = true,
    onDismissListener: (() -> Unit)? = null,
    onShowListener: (() -> Unit)? = null,
) {
    if (this.isDetached || this.isHidden || this.context == null) {
        return
    }
    val successAlert = SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE)
        .showCancelButton(false)

    successAlert.showContentText(content != null)
    successAlert.setCancelable(cancellable)
    successAlert.contentText = content
    successAlert.titleText = titleText
    successAlert.confirmText = confirmText
    successAlert.setBackground(ColorDrawable(Color.TRANSPARENT))
    val oldForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.view?.foreground
    } else {
        this.view?.background
    }
    successAlert.setOnDismissListener {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.view?.foreground = oldForeground
        }
        onDismissListener?.invoke()
        activity?.takeIf {
            it is BaseActivity<*>
        }?.let {
            it as BaseActivity<*>
        }?.onDialogDismiss()
    }
    successAlert.setOnShowListener {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.view?.foreground = context?.let { ctx ->
                ContextCompat.getDrawable(ctx, com.kt.apps.resources.R.drawable.base_background_player_container_error)
            }
        }
        successAlert.getButton(cn.pedant.Sweetalert.R.id.confirm_button).requestFocus()
        onShowListener?.invoke()
        activity?.takeIf {
            it is BaseActivity<*>
        }?.let {
            it as BaseActivity<*>
        }?.onDialogShowing()
    }
    successAlert.show()
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when(event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Logger.d(this@showErrorDialog, message = "OnPauseCalled")
                    successAlert.dismissWithAnimation()
                    lifecycle.removeObserver(this)
                }

                else -> {

                }
            }
        }
    })
    Handler(Looper.getMainLooper()).postDelayed({ onSuccessListener?.let { it() } }, (delayMillis ?: 1900).toLong())
}

fun Activity.showSuccessDialog(
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = true
) {
    try {
        if (this.isDestroyed || this.isFinishing) {
            return
        }
        showSweetDialog(SweetAlertDialog.SUCCESS_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
    } catch (_: Exception) {
    }
}

fun Activity.showErrorDialog(
    onSuccessListener: (() -> Unit)? = null,
    content: String? = null,
    titleText: String? = null,
    confirmText: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = false,
    cancellable: Boolean = true,
    onDismissListener: (() -> Unit)? = null,
    onShowListener: (() -> Unit)? = null,
) {
        if (this.isDestroyed || this.isFinishing) {
            return
        }
        val successAlert = SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
            .showCancelButton(false)

        successAlert.showContentText(content != null)
        successAlert.setCancelable(cancellable)
        successAlert.contentText = content
        successAlert.titleText = titleText
        successAlert.confirmText = confirmText
        successAlert.showCancelButton(true)
            .setCancelText(getString(com.kt.apps.resources.R.string.dialog_update_new_version_title))
            .setCancelClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")))
            }
        successAlert.setBackground(ColorDrawable(Color.TRANSPARENT))

        successAlert.setOnDismissListener {
            onDismissListener?.invoke()
            (this as? BaseActivity<*>)?.run { this.onDialogDismiss() }
        }
        successAlert.setOnShowListener {
            onShowListener?.invoke()
            successAlert.getButton(cn.pedant.Sweetalert.R.id.confirm_button)
                .requestFocus()

            (this as? BaseActivity<*>)?.run { this.onDialogShowing() }
        }
        successAlert.show()
        (this as? FragmentActivity)?.let {
            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            Logger.d(this@showErrorDialog, message = "OnPauseCalled")
                            successAlert.dismissWithAnimation()
                            lifecycle.removeObserver(this)
                        }

                        else -> {

                        }
                    }
                }
            })
        }


        Handler(Looper.getMainLooper()).postDelayed(
            { onSuccessListener?.let { it() } },
            (delayMillis ?: 1900).toLong()
        )
}
@JvmOverloads
fun Activity.showSweetDialog(
    type: Int,
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = false,
    titleText: String? = null,
    confirmText: String? = null
) {
    val successAlert = SweetAlertDialog(this, type)
        .showCancelButton(false)
        .hideConfirmButton()

    successAlert.showContentText(content != null)
    successAlert.setCancelable(!autoDismiss)
    successAlert.contentText = content
    successAlert.titleText = titleText
    successAlert.confirmText = confirmText
    successAlert.setBackground(ColorDrawable(Color.TRANSPARENT))
    successAlert.setOnDismissListener {
        (this as? BaseActivity<*>)?.run { this.onDialogDismiss() }
    }
    successAlert.setOnShowListener {
        successAlert.getButton(cn.pedant.Sweetalert.R.id.confirm_button)
            .requestFocus()

        (this as? BaseActivity<*>)?.run { this.onDialogShowing() }
    }
    successAlert.show()
    if (autoDismiss) {
        Handler(Looper.getMainLooper()).postDelayed({ successAlert.dismissWithAnimation() }, 1500)
    } else {
        if (this is FragmentActivity) {
            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when(event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            try {
                                Logger.d(this@showSweetDialog , message = "OnPauseCalled")
                                successAlert.dismissWithAnimation()
                                lifecycle.removeObserver(this)
                            } catch (_: Exception) {
                            }
                        }

                        else -> {

                        }
                    }
                }
            })
        }
    }
    Handler(Looper.getMainLooper()).postDelayed({ onSuccessListener?.let { it() } }, (delayMillis ?: 1900).toLong())
}

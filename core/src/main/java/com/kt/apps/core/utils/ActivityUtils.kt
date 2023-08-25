package com.kt.apps.core.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.pedant.SweetAlert.SweetAlertDialog
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
    }
    successAlert.setOnShowListener {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.view?.foreground = context?.let { ctx ->
                ContextCompat.getDrawable(ctx, com.kt.apps.resources.R.drawable.base_background_player_container_error)
            }
        }
        onShowListener?.invoke()
    }
    successAlert.show()
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when(event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Logger.d(this, message = "OnPauseCalled")
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
    autoDismiss: Boolean = false
) {
    try {
        if (this.isDestroyed || this.isFinishing) {
            return
        }
        showSweetDialog(
            SweetAlertDialog.ERROR_TYPE,
            onSuccessListener,
            content,
            delayMillis,
            autoDismiss,
            titleText,
            confirmText
        )
    } catch (_: Exception) {
    }
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
                                Logger.d(this, message = "OnPauseCalled")
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

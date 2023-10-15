package com.kt.apps.media.mobile.ui.fragments.dialog

import android.app.Dialog
import android.os.Bundle
import com.kt.apps.core.base.BaseDialogFragment

class CustomDialogFragment(private val dialogView: Dialog): BaseDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogView
    }
}
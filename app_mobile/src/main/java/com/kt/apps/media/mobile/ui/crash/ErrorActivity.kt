package com.kt.apps.media.mobile.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.utils.GlobalExceptionHandler

class ErrorActivity : AppCompatActivity() {

    private val copyButton: MaterialButton? by lazy {
        findViewById(R.id.copy_button)
    }

    private val shareButton: MaterialButton? by lazy {
        findViewById(R.id.share_button)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        copyButton?.setOnClickListener { onClickCopy() }
        shareButton?.setOnClickListener { onClickShare() }
    }

    private fun onClickCopy() {
        val file = GlobalExceptionHandler.crashFile(context = this)
        val str = file.readText()

        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.run {
            setPrimaryClip(ClipData.newPlainText("Debug", str))
            Toast.makeText(this@ErrorActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        } ?: kotlin.run {
            Toast.makeText(this@ErrorActivity, "Cannot copy!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickShare() {
        val file = GlobalExceptionHandler.crashFile(context = this)
        val intent = ShareCompat.IntentBuilder(this)
            .setType("application/txt")
            .setStream(file.toUri())
            .setText(file.readText())
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(intent)
    }
}
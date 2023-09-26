package com.kt.apps.media.mobile.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.kt.apps.media.mobile.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Thread.UncaughtExceptionHandler
import javax.inject.Inject

class GlobalExceptionHandler @Inject constructor(val context: Context): UncaughtExceptionHandler {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    fun activate() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val b = StringBuilder()

        b.append("Build : ${BuildConfig.VERSION_NAME}\n")
        b.append("Version : ${BuildConfig.VERSION_CODE}\n")
        b.append("Phone : ${Build.MODEL.trim()} (${Build.VERSION.INCREMENTAL} ${Build.VERSION.RELEASE} ${Build.VERSION.CODENAME})\n")

        b.append("Memory statuses \n")

        var freeSize = 0L
        var totalSize = 0L
        var usedSize = -1L
        try {
            val info = Runtime.getRuntime()
            freeSize = info.freeMemory()
            totalSize = info.totalMemory()
            usedSize = totalSize - freeSize
        } catch (e: Exception) {
            e.printStackTrace()
        }

        b.append("usedSize   " + usedSize / 1048576L + " MB\n")
        b.append("freeSize   " + freeSize / 1048576L + " MB\n")
        b.append("totalSize   " + totalSize / 1048576L + " MB\n")

        b.append("Thread: ")
        b.append(thread.name)

        b.append(", Exception: ")

        val sw = StringWriter()
        val pw = PrintWriter(sw, true)
        throwable.printStackTrace(pw)
        b.append(sw.buffer.toString())

        val bugDescription = b.toString()
        Log.d("GlobalExceptionHandler", "uncaughtException: $bugDescription")
        saveCrashReport(bugDescription)
    }

    private fun saveCrashReport(crashDescription: String) {
        val crashFile = crashFile(context)
        Log.d("GlobalExceptionHandler", "saveCrashReport: ${crashFile.absolutePath}")
        if (crashFile.exists()) {
            crashFile.delete()
        }

        if (crashDescription.isNotEmpty()) {
            try {
                crashFile.writeText(crashDescription)
            } catch (e: Exception) {
                Log.e("GlobalExceptionHandler", "## saveCrashReport() : fail to write $e")
            }
        }
    }

    companion object {
        // filenames
        private const val LOG_CAT_ERROR_FILENAME = "logcatError.log"
        private const val LOG_CAT_FILENAME = "logcat.log"
        private const val LOG_CAT_SCREENSHOT_FILENAME = "screenshot.png"
        const val CRASH_FILENAME = "imedia_crash.log"

        fun crashFile(context: Context): File {
            return File(context.filesDir.absolutePath, CRASH_FILENAME)
        }
    }
}
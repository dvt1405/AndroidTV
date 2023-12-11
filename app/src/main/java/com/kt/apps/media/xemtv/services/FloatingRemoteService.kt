package com.kt.apps.media.xemtv.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.R
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnTouchRangeListener
import com.lzf.easyfloat.utils.DragUtils
import com.lzf.easyfloat.widget.BaseSwitchView

class FloatingRemoteService : Service() {

    companion object {
        private const val CHANNEL_ID = "Floating Remote"
        private const val ID = 1234
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        showNotification()
        super.onStartCommand(intent, flags, startId)
        showView()
        return START_STICKY
    }

    private fun showView() {

        val tag = "REMOTEVIEW"
        EasyFloat.with(this.applicationContext)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_SIDE)
            .setTag(tag)
            .setLayout(R.layout.remote_view) {
//                val binding = RemoteViewBinding.bind(it)
                configView(it)
            }
            .registerCallback {
                dismiss { }
                drag { view, motionEvent ->
                    DragUtils.registerDragClose(motionEvent, object : OnTouchRangeListener {
                        override fun touchInRange(inRange: Boolean, view: BaseSwitchView) {

                        }

                        override fun touchUpInRange() {
                            EasyFloat.dismiss(tag)
                        }

                    })
                }
            }
            .show()
    }

    private fun configView(binding: View) {
        fun sendKeyEvent(keyEvent: Int) {
            val view = App.get().currentActivity?.window?.decorView?.rootView ?: return
            val inputConnection = BaseInputConnection(view, true)
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
        }

        val mapUIEvent = mapOf(
            R.id.up_btn to KeyEvent.KEYCODE_DPAD_UP,
            R.id.left_btn to KeyEvent.KEYCODE_DPAD_LEFT,
            R.id.right_btn to KeyEvent.KEYCODE_DPAD_RIGHT,
            R.id.down_btn to KeyEvent.KEYCODE_DPAD_DOWN,
            R.id.ok_btn to KeyEvent.KEYCODE_ENTER
        )

        mapUIEvent.keys.forEach { id ->
            binding.findViewById<Button>(id).setOnClickListener {
                val keycode = mapUIEvent[id] ?: return@setOnClickListener
                sendKeyEvent(keycode)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this)
                .createNotificationChannelsCompat(
                    listOf(
                        NotificationChannelCompat.Builder(
                            CHANNEL_ID,
                            NotificationManagerCompat.IMPORTANCE_LOW
                        )
                            .setName("Floating Remote Notification")
                            .setDescription("Floating Remote Description")
                            .build()
                    )
                )
        }
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Remote")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(ID, notification)
        startForeground(ID, notification)
    }
}
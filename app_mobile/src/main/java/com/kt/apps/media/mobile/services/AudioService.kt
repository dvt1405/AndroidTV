package com.kt.apps.media.mobile.services

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleService
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.services.media.NOW_PLAYING_CHANNEL_ID
import com.kt.apps.media.mobile.services.media.NOW_PLAYING_NOTIFICATION_ID
import java.lang.ref.WeakReference

class AudioService : LifecycleService() {
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionConnector: MediaSessionConnector? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val exoPlayerManager: ExoPlayerManagerMobile by lazy {
        App.get()
            .coreComponents
            .exoPlayerManager()
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(applicationContext, "imedia_audio")
            .apply {
                isActive = true
            }
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID
        )
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(false)
                    }
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            })
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .build()
            .apply {
                setUseStopAction(true)
                setPlayer(exoPlayerManager.exoPlayer)
            }
        mediaSession?.sessionToken?.run {
            playerNotificationManager?.setMediaSessionToken(this)
        }

        mediaSessionConnector = mediaSession?.let { MediaSessionConnector(it) }
            ?.apply {
//                setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
//                    override fun getMediaDescription(
//                        player: Player,
//                        windowIndex: Int
//                    ): MediaDescriptionCompat {
//                        val currentItem = player.currentMediaItem
//                        if (currentItem != null) {
//                            return MediaDescriptionCompat.Builder()
//                                .setTitle(currentItem.mediaMetadata.title)
//                                .setMediaId(currentItem.mediaId)
//                                .setExtras(currentItem.mediaMetadata.extras)
//                                .build()
//                        }
//                        return MediaDescriptionCompat.Builder().build()
//                    }
//                })

                setPlayer(exoPlayerManager.exoPlayer)
            }
    }

    fun attachPlayer(player: ExoPlayer?) {
        mediaSessionConnector?.setPlayer(player)
        playerNotificationManager?.setPlayer(player)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        handleIntent(intent)
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)
        Log.d(TAG, "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSessionConnector?.setPlayer(null)
        playerNotificationManager?.setPlayer(null)

        exoPlayerManager.detach()

        super.onDestroy()
    }

    @MainThread
    private fun handleIntent(intent: Intent?) {

    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@AudioService

        val exoPlayer
            get() = this@AudioService.exoPlayerManager.exoPlayer
    }
}

class PlayerServiceConnection : ServiceConnection {

    private var _service = WeakReference<AudioService?>(null)

    var service: AudioService?
        get() {
            return _service.get()
        }
        set(value) {}

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "onServiceConnected: ")
        val localBinder = service as? AudioService.LocalBinder
        _service = WeakReference(localBinder?.service)

    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }
}
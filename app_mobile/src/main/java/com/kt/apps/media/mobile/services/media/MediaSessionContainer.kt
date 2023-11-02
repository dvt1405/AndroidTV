package com.kt.apps.media.mobile.services.media

import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import androidx.core.app.NotificationManagerCompat
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.di.AppScope
import javax.inject.Inject


@AppScope
class MediaSessionContainer @Inject constructor(
    private val app: App,
    private val exoPlayerManager: ExoPlayerManagerMobile
) {
    private val mediaBrowser: MediaBrowserCompat by lazy {
        MediaBrowserCompat(
            app.applicationContext,
            ComponentName(app.applicationContext, IMediaSessionService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    super.onConnected()
                }
            },
            null
        )
    }

    private var _cachePlayingState: Boolean? = null
    fun onStart() {
        if (NotificationManagerCompat.from(app.applicationContext).areNotificationsEnabled()) {
            if (!mediaBrowser.isConnected) {
                mediaBrowser.connect()
            }
        }
    }

    fun stop() {
        _cachePlayingState = null
        exoPlayerManager.exoPlayer?.run {
            pause()
        }
    }

    fun onDestroy() {
        mediaBrowser.disconnect()
    }

    fun onStop() {
        if (mediaBrowser.isConnected) {
            return
        }
        exoPlayerManager.exoPlayer?.run {
            _cachePlayingState = isPlaying
            pause()
        }
    }

    fun onResume() {
        if (mediaBrowser.isConnected) {
            return
        }
        exoPlayerManager.exoPlayer?.run {
            if (_cachePlayingState == true) {
                play()
            }
            _cachePlayingState = null
        }
    }
}


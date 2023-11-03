package com.kt.apps.media.mobile.services.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.loadImgBitmapByResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.complex.ComplexActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val NOW_PLAYING_CHANNEL_ID = "iMedia"
const val NOW_PLAYING_NOTIFICATION_ID = 1234

class IMediaNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {
    fun hideNotification() {
        notificationManager.setPlayer(null)

    }

    fun showNotificationForPlayer(exoPlayer: ExoPlayer) {
        notificationManager.setPlayer(exoPlayer)

    }

    private var notificationManager: PlayerNotificationManager

    init {
        notificationManager =
            PlayerNotificationManager.Builder(
                context,
                NOW_PLAYING_NOTIFICATION_ID,
                NOW_PLAYING_CHANNEL_ID
            )
                .setMediaDescriptionAdapter(object : MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        return player.mediaMetadata.displayTitle ?: ""
                    }

                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        val intent = Intent(context, ComplexActivity::class.java)
                        return PendingIntent.getActivity(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    }

                    override fun getCurrentContentText(player: Player): CharSequence? {
                        return player.mediaMetadata.albumTitle
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {
                        val uri = player.mediaMetadata.artworkUri?.toString()
                        CoroutineScope(Dispatchers.Default).launch {
                            val bitmap = uri?.let {
                                loadImgBitmapByResName(context, it)
                            } ?: Glide
                                .with(context)
                                .asBitmap()
                                .load(R.mipmap.ic_launcher)
                                .submit()
                                .get()
                            callback.onBitmap(bitmap)
                        }
                        return null
                    }

                })
                .setPlayActionIconResourceId(R.drawable.ic_play)
                .setPauseActionIconResourceId(R.drawable.ic_pause)
                .setStopActionIconResourceId(R.drawable.ic_clear_24)
                .setCustomActionReceiver(object : PlayerNotificationManager.CustomActionReceiver {
                    override fun createCustomActions(
                        context: Context,
                        instanceId: Int
                    ): MutableMap<String, NotificationCompat.Action> {
                        Logger.d(this@IMediaNotificationManager, message = "createCustomActions")
                        return mutableMapOf(
                            "VoiceAssistant" to NotificationCompat.Action(
                                androidx.leanback.R.drawable.lb_ic_search_mic,
                                "VoiceAssistant",
                                PendingIntent.getActivity(
                                    context,
                                    1,
                                    Intent(context, ComplexActivity::class.java).apply {
                                        data =
                                            Uri.parse("${Constants.SCHEME_DEFAULT}://${Constants.HOST_VOICE}/search")
                                    },
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                        )
                    }

                    override fun getCustomActions(player: Player): MutableList<String> {
                        Log.e("TAG", "getCustomActions")
                        return mutableListOf(
                            "VoiceAssistant"
                        )
                    }

                    override fun onCustomAction(player: Player, action: String, intent: Intent) {
                        Log.e("TAG", "onCustomAction $action")
                    }

                })
                .setNotificationListener(notificationListener)
                .build()

        notificationManager.setMediaSessionToken(sessionToken)
        notificationManager.setSmallIcon(R.mipmap.ic_launcher)
        notificationManager.setUseNextAction(false)
        notificationManager.setUsePreviousAction(false)
        notificationManager.setUseFastForwardAction(false)
        notificationManager.setUseRewindAction(false)
        notificationManager.setUseStopAction(true)

    }

}
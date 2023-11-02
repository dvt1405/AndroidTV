package com.kt.apps.media.mobile.services.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.loadImgBitmapByResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.complex.ComplexActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                            } ?:  Glide
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
                .setCustomActionReceiver(object : PlayerNotificationManager.CustomActionReceiver {
                override fun createCustomActions(
                    context: Context,
                    instanceId: Int
                ): MutableMap<String, NotificationCompat.Action> {
                    Logger.d(this@IMediaNotificationManager, message = "createCustomActions")
                    return mutableMapOf(
//                        "VoiceAssistant" to NotificationCompat.Action(
//                            androidx.leanback.R.drawable.lb_ic_search_mic,
//                            "VoiceAssistant",
//                            PendingIntent.getBroadcast(
//                                context,
//                                0,
//                                Intent("kiki").apply {
//                                    this.`package` = context.packageName
//                                },
//                                if (VERSION.SDK_INT >= VERSION_CODES.M) {
//                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                                } else {
//                                    0
//                                }
//                            )
//                        )
                    )
                }

                override fun getCustomActions(player: Player): MutableList<String> {
                    Log.e("TAG", "getCustomActions")
                    return mutableListOf(
//                        "VoiceAssistant"
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

    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return controller.sessionActivity
        }

        override fun getCurrentContentText(player: Player): String {
            return controller.metadata.description.subtitle.toString()
        }

        override fun getCurrentContentTitle(player: Player): String {
            return controller.metadata.description.title.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri ?: return Glide
                .with(context)
                .asBitmap()
                .load(R.mipmap.ic_launcher)
                .submit()
                .get()
            return if (currentIconUri != iconUri || currentBitmap == null) {
                currentIconUri = iconUri
                resolveUriAsBitmap(iconUri)
                null
            } else {
                currentBitmap
            }
        }

        private fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return Glide.with(context).applyDefaultRequestOptions(glideOptions)
                .asBitmap()
                .load(uri)
                .submit()
                .get()
        }
    }

    private val glideOptions = RequestOptions()
        .fallback(R.mipmap.ic_launcher)
        .diskCacheStrategy(DiskCacheStrategy.DATA)

}
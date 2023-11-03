package com.kt.apps.media.mobile.services.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.R
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

class IMediaSessionService : MediaBrowserServiceCompat(), HasAndroidInjector {
    private val disposable by lazy {
        CompositeDisposable()
    }
    private val packageValidator by lazy {
        PackageValidator(this, R.xml.allowed_media_browser_callers)
    }
    private var currentPlaylistItems: MutableList<TVChannel> = mutableListOf()
    private val notificationManager: IMediaNotificationManager by lazy {
        IMediaNotificationManager(
            this,
            mediaSession.sessionToken,
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing && !isForegroundService) {
                        ContextCompat.startForegroundService(
                            applicationContext,
                            Intent(applicationContext, this@IMediaSessionService.javaClass)
                        )

                        startForeground(notificationId, notification)
                        isForegroundService = true
                    }
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopForeground(true)
                    isForegroundService = false
                    stopSelf()
                }
            }
        )
    }

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile
    private var isForegroundService = false
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        val sessionActivityPendingIntent =
            packageManager!!.getLaunchIntentForPackage(packageName)!!.let { sessionIntent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(
                        this,
                        2,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    PendingIntent.getActivity(this, 2, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
                }
            }

        mediaSession = MediaSessionCompat(this, "IMediaSessionService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onCustomAction(action: String?, extras: Bundle?) {
                super.onCustomAction(action, extras)
                Log.e("TAG", "Custom action")
            }
        })
        sessionToken = mediaSession.sessionToken
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        val mediaPreparer = IMediaSessionPreparer(
            exoPlayerManager,
            currentPlaylistItems
        )
        mediaSessionConnector.setPlaybackPreparer(mediaPreparer)
        mediaSessionConnector.setEnabledPlaybackActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE
        )
        mediaSessionConnector.setQueueNavigator(IMediaSessionQueueNavigator(mediaSession, currentPlaylistItems))
        mediaSessionConnector.setCustomActionProviders(object : MediaSessionConnector.CustomActionProvider {
            override fun onCustomAction(player: Player, action: String, extras: Bundle?) {
            }

            override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                return PlaybackStateCompat.CustomAction.Builder(
                    "Mic",
                    "Mic",
                    androidx.leanback.R.drawable.lb_ic_search_mic
                )
                    .setExtras(Bundle())
                    .build()
            }
        })
        mediaSessionConnector.setPlayer(exoPlayerManager.exoPlayer)

        currentPlaylistItems.clear()

        exoPlayerManager.registerPlayerAttachedObserver("IMediaSessionService") { playbackState ->
            when (playbackState) {
                Player.STATE_READY -> {
                    mediaSessionConnector.setPlayer(exoPlayerManager.exoPlayer)
                    exoPlayerManager.exoPlayer?.run {
                        notificationManager.showNotificationForPlayer(this)
                    } ?: kotlin.run {
                        notificationManager.hideNotification()
                    }
                }

                else -> {
                    mediaSessionConnector.setPlayer(null)
                    notificationManager.hideNotification()
                }
            }

        }
    }
    override fun onDestroy() {
        super.onDestroy()
        exoPlayerManager.unRegisterPlayerAttachedObserver("IMediaSessionService")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        val rootExtras = Bundle().apply {
            putBoolean(
                MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED,
                true
            )
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            )
            putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            )
        }

        val isRecentRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        val browserRootPath = if (isRecentRequest) {
            MEDIA_RECENT_ROOT
        } else {
            MEDIA_RADIO_ROOT
        }

        return if (isKnownCaller) {
            BrowserRoot(browserRootPath, rootExtras)
        } else {
            BrowserRoot("Empty", rootExtras)
        }

    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (true) {
            result.sendResult(mutableListOf<MediaBrowserCompat.MediaItem>())
            return
        }
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        super.onSearch(query, extras, result)
    }

    private fun List<TVChannel>.mapToMediaItems(context: Context) = this.map {
        MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(it.channelId)
                .setTitle(it.tvChannelName)
                .setDescription(it.tvGroupLocalName)
                .setIconBitmap(
                    GlideApp.with(context)
                        .asBitmap()
                        .load(it.logoChannel)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .submit()
                        .get()
                )
                .setExtras(
                    bundleOf(
                        "url" to it.tvChannelWebDetailPage,
                        "logo" to it.logoChannel
                    )
                )
                .build(),
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
    }


    companion object {
        const val MEDIA_BROWSER_ROOT_ID = "extra:browser_root_id"
        const val MEDIA_RECOMMENDED_ROOT = "__RECOMMENDED__"
        const val MEDIA_RECENT_ROOT = "__RECENT__"
        const val MEDIA_RADIO_ROOT = "__RADIO__"

    }

    override fun androidInjector(): AndroidInjector<Any> {
        return (application as CoreApp).androidInjector()
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@IMediaSessionService

    }

}
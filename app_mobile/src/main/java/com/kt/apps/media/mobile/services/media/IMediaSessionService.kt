package com.kt.apps.media.mobile.services.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.Util
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.usecase.GetListTVChannel
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.R
import io.reactivex.rxjava3.disposables.CompositeDisposable

class IMediaSessionService : MediaBrowserServiceCompat() {
    private val disposable by lazy {
        CompositeDisposable()
    }
    private val tvDataSource: GetListTVChannel by lazy {
        App.get()
            .tvComponents
            .getListTVChannel()
    }
    private val packageValidator by lazy {
        PackageValidator(this, R.xml.allowed_media_browser_callers)
    }
    private var currentMediaItemIndex: Int = 0
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

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(true)
                    isForegroundService = false
                    stopSelf()
                }
            }
        )
    }
    private val exoPlayerManager: ExoPlayerManagerMobile by lazy {
        App.get()
            .coreComponents
            .exoPlayerManager()
    }

    private val exoPlayerEventListener by lazy {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_BUFFERING,
                    Player.STATE_READY -> {
                        exoPlayerManager.exoPlayer?.let {
                            notificationManager.showNotificationForPlayer(exoPlayer = it)
                        }
                        if (playbackState == Player.STATE_READY) {
                            saveRecentSongToStorage()

                            if (exoPlayerManager.exoPlayer?.playWhenReady == false) {
                                stopForeground(false)
                                isForegroundService = false
                            }
                        }
                    }
                    else -> {
                        notificationManager.hideNotification()
                    }
                }
            }


            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                    || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                    || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                ) {
                    currentMediaItemIndex = if (currentPlaylistItems.isNotEmpty()) {
                        Util.constrainValue(
                            player.currentMediaItemIndex,
                            0,
                            currentPlaylistItems.size - 1
                        )
                    } else {
                        0
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                    || error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                ) {
                    Logger.e(
                        this@IMediaSessionService, message = "Player error:{" +
                                "${error.errorCode}, " +
                                "${error.message}" +
                                "}"
                    )
                }

            }
        }
    }

    private var isForegroundService = false
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()
        val sessionActivityPendingIntent =
            packageManager!!.getLaunchIntentForPackage(packageName)!!.let { sessionIntent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(
                        this,
                        2,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this,
                        2,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
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
        val loadChannelTask = tvDataSource.invoke(false, TVDataSourceFrom.GG)
            .subscribe({ channelList ->
                currentPlaylistItems.addAll(channelList)
            }, {
            }, {
            })
        disposable.add(loadChannelTask)

    }


    private fun saveRecentSongToStorage() {

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
        val finalMediaItemList = mutableListOf<MediaBrowserCompat.MediaItem>()
        when (parentId) {
            MEDIA_RECENT_ROOT -> {
                val loadChannelTask = tvDataSource.invoke(false, TVDataSourceFrom.GG)
                    .subscribe({ channelList ->
                        finalMediaItemList.addAll(channelList.mapToMediaItems(this))
                        result.sendResult(finalMediaItemList)
                    }, {
                    }, {
                        result.sendResult(finalMediaItemList)
                    })

                disposable.add(loadChannelTask)
            }

            MEDIA_RADIO_ROOT -> {
                val rootList = mutableListOf<MediaBrowserCompat.MediaItem>()
                rootList.add(
                    MediaBrowserCompat.MediaItem(
                        MediaDescriptionCompat.Builder()
                            .setMediaId("VOV")
                            .setTitle("VOV")
                            .setDescription("Kên VOV Việt Nam")
                            .setIconBitmap(
                                GlideApp.with(this)
                                    .asBitmap()
                                    .load(com.kt.apps.core.R.drawable.icon_channel_vovtv_1656559082882)
                                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                                    .submit()
                                    .get()
                            )
                            .build(),
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    ),

                    )
                MediaMetadataCompat.Builder()

                    .build()
                rootList.add(
                    MediaBrowserCompat.MediaItem(
                        MediaDescriptionCompat.Builder()
                            .setMediaId("VOH")
                            .setTitle("VOH")
                            .setDescription("Kênh VOH Việt Nam")
                            .setIconBitmap(
                                GlideApp.with(this)
                                    .asBitmap()
                                    .load(com.kt.apps.core.R.drawable.icon_channel_voh)
                                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                                    .submit()
                                    .get()
                            )
                            .build(),
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    ),

                    )

                result.sendResult(rootList)
            }

            "VOV" -> {
                currentPlaylistItems.clear()
                val loadChannelTask = tvDataSource.invoke(false, TVDataSourceFrom.GG)
                    .map {
                        currentPlaylistItems.addAll(it)
                        it.filter {
                            it.tvGroup == TVChannelGroup.VOV.name
                        }
                    }
                    .subscribe({ channelList ->
                        finalMediaItemList.addAll(channelList.mapToMediaItems(this))
                        result.sendResult(finalMediaItemList)
                    }, {
                    }, {
                        result.sendResult(finalMediaItemList)
                    })
                disposable.add(loadChannelTask)
            }

            "VOH" -> {
                currentPlaylistItems.clear()
                val loadChannelTask = tvDataSource.invoke(false, TVDataSourceFrom.GG)
                    .map {
                        currentPlaylistItems.addAll(it)
                        it.filter {
                            it.tvGroup == TVChannelGroup.VOH.name
                        }
                    }
                    .subscribe({ channelList ->
                        finalMediaItemList.addAll(channelList.mapToMediaItems(this))
                        result.sendResult(finalMediaItemList)
                    }, {
                    }, {
                        result.sendResult(finalMediaItemList)
                    })
                disposable.add(loadChannelTask)
            }

            else -> {

            }
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

}
package com.kt.apps.core.base.player

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaDrm
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.kt.apps.core.Constants
import com.kt.apps.core.R
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.trustEveryone
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

abstract class AbstractExoPlayerManager(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager,
    private val _historyManager: IMediaHistoryRepository
) : Application.ActivityLifecycleCallbacks, AudioFocusManager.OnFocusChange {

    protected var mExoPlayer: ExoPlayer? = null

    private val _playerListenerObserver by lazy {
        mutableListOf<(() -> Unit)>()
    }

    private val _audioAttr by lazy {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
                }
            }
            .build()
    }

    protected val playerListener by lazy {
        object : Player.Listener {

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_IS_LOADING_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED
                    )
                ) {
                    val mediaItem = player.currentMediaItem ?: return
                    if (player.contentDuration > 2 * 60_000 && player.contentPosition > 60_000) {
                        val historyMediaItemDTO = HistoryMediaItemDTO.mapFromMediaItem(
                            mediaItem,
                            player.contentPosition,
                            player.contentDuration,
                            type = getMediaSourceTypeFromExtras(mediaItem)
                        )
                        _historyManager.saveHistoryItem(historyMediaItemDTO)
                    }
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    ExoPlayer.STATE_IDLE -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_IDLE")
                    }

                    ExoPlayer.STATE_BUFFERING -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_BUFFERING")
                    }

                    ExoPlayer.STATE_READY -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_READY")
                    }

                    ExoPlayer.STATE_ENDED -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_ENDED")
                    }

                    else -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                mExoPlayer = null
                Logger.d(
                    this@AbstractExoPlayerManager,
                    message = error.message?.plus(error.errorCodeName) ?: error.errorCodeName
                )
            }

        }
    }

    private fun getMediaSourceTypeFromExtras(mediaItem: MediaItem): HistoryMediaItemDTO.Type {
        mediaItem.mediaMetadata.extras?.let { bundel ->
            bundel.keySet().forEach {
                Logger.d(this, message = "key: $it, value: ${bundel[it]}")
            }
        }
        val type = try {
            HistoryMediaItemDTO.Type.valueOf(
                mediaItem.mediaMetadata.extras?.getString(
                    EXTRA_MEDIA_SOURCE_TYPE,
                    HistoryMediaItemDTO.Type.IPTV.name
                ) ?: HistoryMediaItemDTO.Type.IPTV.name
            )
        } catch (e: Exception) {
            HistoryMediaItemDTO.Type.IPTV
        }
        return type
    }

    init {
        _application.registerActivityLifecycleCallbacks(this)
    }


    val exoPlayer: ExoPlayer?
        get() = mExoPlayer

    fun addListener() {
        _playerListenerObserver.add {
        }
    }

    open fun prepare() {
        saveHistory()
        mExoPlayer?.stop()
        mExoPlayer?.release()
        mExoPlayer = buildExoPlayer()
    }

    protected fun buildExoPlayer() = ExoPlayer.Builder(_application)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setAudioAttributes(_audioAttr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    fun pause() {
        Logger.d(this, message = "Pause")
        mExoPlayer?.pause()
    }

    open fun getMediaSource(
        data: List<LinkStream>,
        itemMetaData: Map<String, String>?,
        isHls: Boolean,
        headers: Map<String, String>? = null
    ): List<MediaSource> {
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        val defaultHeader = getDefaultHeaders(data.first().referer.ifEmpty {
            data.first().m3u8Link.getBaseUrl()
        }, data.first())
        headers?.let { prop -> defaultHeader.putAll(prop) }
        dfSource.setKeepPostFor302Redirects(true)
        dfSource.setAllowCrossProtocolRedirects(true)
        if (!defaultHeader.contains("user-agent")) {
            defaultHeader["user-agent"] = _application.getString(R.string.user_agent)
        }
        dfSource.setUserAgent(defaultHeader["user-agent"])
        dfSource.setDefaultRequestProperties(defaultHeader)
        return data.map { linkStream ->
            if (isHls || linkStream.m3u8Link.contains(".m3u8")) {
                Logger.d(this, "HlsMediaSource", "HlsMediaSource: $linkStream")
                HlsMediaSource.Factory(dfSource)
                    .createMediaSource(
                        createMediaItem(linkStream, itemMetaData, defaultHeader)
                    )
            } else if (linkStream.m3u8Link.contains(".mpd")) {
                Logger.d(this, "MediaSource", "DashMediaSource: $linkStream")
                val licenseUriStr = headers?.get("inputstream.adaptive.license_key")?.ifEmpty {
                    linkStream.m3u8Link
                }.also {
                    Logger.d(this@AbstractExoPlayerManager, message = "license_key: $it")
                } ?: linkStream.m3u8Link
                val licenseUri = Uri.parse(licenseUriStr)
                val mediaDrmCallback = object : MediaDrmCallback {
                    override fun executeProvisionRequest(
                        uuid: UUID,
                        request: ExoMediaDrm.ProvisionRequest
                    ): ByteArray {
                        return byteArrayOf()
                    }

                    override fun executeKeyRequest(
                        uuid: UUID,
                        request: ExoMediaDrm.KeyRequest
                    ): ByteArray {
                        return byteArrayOf()
                    }
                }

                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        C.WIDEVINE_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(mediaDrmCallback)

                val mediaSourceFactory: DashMediaSource.Factory = DashMediaSource.Factory(dfSource)
                mediaSourceFactory.setDrmSessionManagerProvider {
                    drmSessionManager
                }
                mediaSourceFactory.createMediaSource(
                    MediaItem.Builder()
                        .setDrmConfiguration(
                            MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                                .setForceDefaultLicenseUri(true)
                                .setScheme(C.CLEARKEY_UUID)
                                .setLicenseUri(licenseUri)
                                .setLicenseRequestHeaders(
                                    mapOf(
                                        "user-agent" to (defaultHeader["user-agent"] ?: Constants.USER_AGENT),
                                        "Host" to "${licenseUri.host}",
                                        "Origin" to "${licenseUri.scheme}://${licenseUri.host}",
                                        "Referer" to "${licenseUri.scheme}://${licenseUri.host}/",
                                        "Sec-Fetch-Dest" to "empty",
                                        "Sec-Fetch-Mode" to "cors",
                                        "Sec-Fetch-Site" to "cross-site",
                                        "Accept" to "*/*",
                                        "Accept-Encoding" to "gzip, deflate, br",
                                        "Accept-Language" to "en-US,en;q=0.9,vi;q=0.8",
                                        "Connection" to "keep-alive"
                                    )
                                )
                                .build()
                        )
                        .setUri(linkStream.m3u8Link)
                        .build()
                )
            } else if (linkStream.m3u8Link.contains(".mp4")) {
                DefaultMediaSourceFactory(dfSource)
                    .createMediaSource(
                        createMediaItem(linkStream, itemMetaData, defaultHeader)
                    )
            } else {
                Logger.d(this, "MediaSource", "ProgressiveMediaSource: $linkStream")
                ProgressiveMediaSource.Factory(dfSource)
                    .createMediaSource(
                        createMediaItem(linkStream, itemMetaData, defaultHeader)
                    )
            }
        }
    }

    open fun playVideo(
        linkStreams: List<LinkStream>,
        isHls: Boolean,
        itemMetaData: Map<String, String>,
        playerListener: Player.Listener? = null,
        headers: Map<String, String>? = null
    ) {
        prepare()
        trustEveryone()
        val mediaSources = getMediaSource(linkStreams, itemMetaData, isHls, headers)
        mExoPlayer?.setMediaSources(mediaSources)
        mExoPlayer?.removeListener(this.playerListener)
        mExoPlayer?.addListener(this.playerListener)
        playerListener?.let {
            mExoPlayer?.removeListener(it)
            mExoPlayer?.addListener(it)
        }
        mExoPlayer?.playWhenReady = true
        mExoPlayer?.prepare()
    }

    private fun createMediaItem(
        linkStream: LinkStream,
        mediaData: Map<String, String>? = null,
        headers: Map<String, String>? = null,
    ): MediaItem {
        val referer = linkStream.referer.ifEmpty {
            linkStream.m3u8Link.getBaseUrl()
        }
        val requestMetadataBundle = bundleOf()
        requestMetadataBundle.putString(EXTRA_MEDIA_REFERER, referer)
        headers?.let {
            for ((key, value) in headers) {
                requestMetadataBundle.putString(key, value)
            }
        }
        val requestMetadata = MediaItem.RequestMetadata.Builder()
            .setMediaUri(Uri.parse("https://live-on-v2-cdnetwork.sigmaott.com/manifest/joyfm/playlist_720p.m3u8"))
            .setExtras(requestMetadataBundle)
            .build()

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(mediaData?.get(EXTRA_MEDIA_TITLE))
            .setAlbumArtist(mediaData?.get(EXTRA_MEDIA_ALBUM_ARTIST))
            .setAlbumTitle(mediaData?.get(EXTRA_MEDIA_ALBUM_TITLE))
            .setArtworkUri(Uri.parse(mediaData?.get(EXTRA_MEDIA_THUMB) ?: ""))
            .setDescription(mediaData?.get(EXTRA_MEDIA_DESCRIPTION))
            .setDisplayTitle(mediaData?.get(EXTRA_MEDIA_TITLE))
            .setExtras(
                bundleOf(
                    EXTRA_MEDIA_ID to (mediaData?.get(EXTRA_MEDIA_ID) ?: linkStream.streamId),
                    EXTRA_MEDIA_TITLE to mediaData?.get(EXTRA_MEDIA_TITLE),
                    EXTRA_MEDIA_ALBUM_ARTIST to mediaData?.get(EXTRA_MEDIA_ALBUM_ARTIST),
                    EXTRA_MEDIA_ALBUM_TITLE to mediaData?.get(EXTRA_MEDIA_ALBUM_TITLE),
                    EXTRA_MEDIA_THUMB to mediaData?.get(EXTRA_MEDIA_THUMB),
                    EXTRA_MEDIA_DESCRIPTION to mediaData?.get(EXTRA_MEDIA_DESCRIPTION),
                    EXTRA_MEDIA_DURATION to mediaData?.get(EXTRA_MEDIA_DURATION),
                    EXTRA_MEDIA_CURRENT_POSITION to mediaData?.get(EXTRA_MEDIA_CURRENT_POSITION),
                    EXTRA_MEDIA_SOURCE_TYPE to mediaData?.get(EXTRA_MEDIA_SOURCE_TYPE),
                    EXTRA_MEDIA_LAST_PLAY_TIME to mediaData?.get(EXTRA_MEDIA_LAST_PLAY_TIME),
                    EXTRA_MEDIA_REFERER to referer,
                    EXTRA_MEDIA_IS_HLS to linkStream.isHls
                )
            )
            .setIsPlayable(true)
            .build()

        return MediaItem.fromUri(linkStream.m3u8Link.trim())
            .buildUpon()
            .setMediaId(mediaData?.get(EXTRA_MEDIA_ID) ?: linkStream.streamId)
            .setRequestMetadata(requestMetadata)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun getDefaultHeaders(referer: String, currentLinkStream: LinkStream): MutableMap<String, String> {
        val needHost = referer.contains("auth_key")
        val host = try {
            referer.trim().toHttpUrl().host
        } catch (e: Exception) {
            ""
        }
        return mutableMapOf(
            "Accept" to "*/*",
            "accept-encoding" to "gzip, deflate, br",
            "origin" to referer.getBaseUrl(),
            "referer" to referer.trim(),
            "sec-fetch-dest" to "empty",
            "sec-fetch-site" to "cross-site",
        ).apply {
            if (needHost) {
                this["Host"] = host
            }
            currentLinkStream.token?.let {
                this["token"] = it
                this["Authorization"] = it
            }
            currentLinkStream.host?.let {
                this["host"] = host
            }
        }
    }

    private fun saveHistory() {
        exoPlayer ?: return
        val mediaItem = exoPlayer?.currentMediaItem ?: return
        if (exoPlayer!!.contentDuration > 2 * 60_000 && exoPlayer!!.contentPosition > 60_000) {
            val historyMediaItemDTO = HistoryMediaItemDTO.mapFromMediaItem(
                mediaItem,
                exoPlayer!!.contentPosition,
                exoPlayer!!.contentDuration,
                type = getMediaSourceTypeFromExtras(mediaItem)
            )
            _historyManager.saveHistoryItem(historyMediaItemDTO, 0L)
        }
    }

    abstract fun detach(listener: Player.Listener? = null)


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (activity::class.java.name.contains("PlaybackActivity")) {
            mExoPlayer?.play()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        saveHistory()
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity::class.java.name.contains("PlaybackActivity")) {
            mExoPlayer?.stop()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onAudioFocus() {
    }

    override fun onAudioLossFocus() {
        mExoPlayer?.pause()
    }

    companion object {
        val defaultHeaders by lazy {
            mapOf(
                "Accept" to "*/*",
            )
        }
        const val EXTRA_MEDIA_ID = "extra:media_id"
        const val EXTRA_LINK_TO_LAY = "extra:link_to_play"
        const val EXTRA_MEDIA_TITLE = "extra:media_title"
        const val EXTRA_MEDIA_ALBUM_TITLE = "extra:media_album_title"
        const val EXTRA_MEDIA_ALBUM_ARTIST = "extra:media_album_artist"
        const val EXTRA_MEDIA_DESCRIPTION = "extra:media_description"
        const val EXTRA_MEDIA_DURATION = "extra:media_duration"
        const val EXTRA_MEDIA_CURRENT_POSITION = "extra:media_current_position"
        const val EXTRA_MEDIA_THUMB = "extra:media_thumb"
        const val EXTRA_MEDIA_SOURCE_TYPE = "extra:media_source_type"
        const val EXTRA_MEDIA_LAST_PLAY_TIME = "extra:media_last_play_time"
        const val EXTRA_MEDIA_REFERER = "extra:referer"
        const val EXTRA_MEDIA_IS_LIVE = "extra:is_live"
        const val EXTRA_MEDIA_IS_HLS = "extra:is_hls"
    }


}
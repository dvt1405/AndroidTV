package com.kt.apps.core.base.player

import android.util.Log
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.gms.cast.framework.CastContext
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.utils.TAG
import javax.inject.Inject


class ExoPlayerManagerMobile @Inject constructor(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager,
    private val historyManager: IMediaHistoryRepository
) : AbstractExoPlayerManager(_application, _audioFocusManager, historyManager) {
    private val castContext by lazy {
        CastContext.getSharedInstance(_application.applicationContext)
    }

    private var _castPlayer: CastPlayer? = null
    val castPlayer: CastPlayer?
        get() = _castPlayer
    override fun prepare() {
        if (exoPlayer == null) {
            mExoPlayer?.stop()
            mExoPlayer?.release()
            mExoPlayer = buildExoPlayer()
        }

        if (_castPlayer == null) {
            _castPlayer = CastPlayer(castContext)
//            _castPlayer?.addListener(this.playerListener)
            _castPlayer?.addListener(object: Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.d(TAG, "onPlayerError: $error")
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    Log.d(TAG, "onPlaybackStateChanged: $playbackState")
                }
            })
        }
    }
    override fun playVideo(
        linkStreams: List<LinkStream>,
        isHls: Boolean,
        itemMetaData: Map<String, String>,
        playerListener: Player.Listener?,
        headers: Map<String, String>?
    ) {
        super.playVideo(linkStreams, isHls, itemMetaData, playerListener, headers)
        mExoPlayer?.play()
    }

    override fun detach(listener: Player.Listener?) {
        if (listener != null) {
            mExoPlayer?.removeListener(listener)
        }
        mExoPlayer?.removeListener(playerListener)
        _audioFocusManager.releaseFocus()
        mExoPlayer?.release()
        mExoPlayer = null
    }

    fun setSessionAvailabilityListener(listener: SessionAvailabilityListener) {
        _castPlayer?.setSessionAvailabilityListener(listener)
    }

}
package com.kt.apps.core.base.player

import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.repository.IMediaHistoryRepository
import javax.inject.Inject


class ExoPlayerManagerMobile @Inject constructor(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager,
    private val historyManager: IMediaHistoryRepository
) : AbstractExoPlayerManager(_application, _audioFocusManager, historyManager) {
//    private val internalPlayerListener = object: Player.Listener {
//        override fun onPlayerError(error: PlaybackException) {
//
//        }
//    }
    override fun prepare() {
        if (exoPlayer == null) {
            mExoPlayer?.stop()
            mExoPlayer?.release()
            mExoPlayer = buildExoPlayer()
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
//        mExoPlayer?.removeListener(internalPlayerListener)
//        mExoPlayer?.addListener(internalPlayerListener)
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


}
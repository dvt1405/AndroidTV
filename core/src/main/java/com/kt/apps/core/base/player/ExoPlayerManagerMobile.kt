package com.kt.apps.core.base.player

import com.google.android.exoplayer2.Player
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreScope
import javax.inject.Inject


class ExoPlayerManagerMobile @Inject constructor(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager
) : AbstractExoPlayerManager(_application, _audioFocusManager) {

    override fun prepare() {
        if (exoPlayer == null) {
            mExoPlayer?.stop()
            mExoPlayer?.release()
            mExoPlayer = buildExoPlayer()
        }
    }
    override fun playVideo(
        data: List<LinkStream>,
        isHls: Boolean,
        playerListener: Player.Listener?,
        headers: Map<String, String>?
    ) {
        super.playVideo(data, isHls, playerListener, headers)
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
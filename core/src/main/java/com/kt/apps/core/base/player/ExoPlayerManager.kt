package com.kt.apps.core.base.player

import com.google.android.exoplayer2.Player
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.leanback.media.LeanbackPlayerAdapter
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.repository.IMediaHistoryRepository
import javax.inject.Inject


class ExoPlayerManager @Inject constructor(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager,
    private val _historyManager: IMediaHistoryRepository
) : AbstractExoPlayerManager(_application, _audioFocusManager, _historyManager) {
    private var _playerAdapter: LeanbackPlayerAdapter? = null

    val playerAdapter: LeanbackPlayerAdapter?
        get() = _playerAdapter

    override fun prepare() {
        if (mExoPlayer == null || _playerAdapter == null) {
            mExoPlayer?.stop()
            mExoPlayer?.release()
            mExoPlayer = buildExoPlayer()
            _playerAdapter = LeanbackPlayerAdapter(_application, mExoPlayer!!, 5)
        }
    }
    override fun onAudioLossFocus() {
        super.onAudioFocus()
        _playerAdapter?.pause()
        Logger.d(this@ExoPlayerManager, message = "onAudioLossFocus")
    }

    override fun detach(listener: Player.Listener?) {
        if (listener != null) {
            mExoPlayer?.removeListener(listener)
        }
        mExoPlayer?.removeListener(this.playerListener)
        _audioFocusManager.releaseFocus()
        _playerAdapter?.onDetachedFromHost()
        mExoPlayer?.release()
        mExoPlayer = null
        _playerAdapter = null
    }

}
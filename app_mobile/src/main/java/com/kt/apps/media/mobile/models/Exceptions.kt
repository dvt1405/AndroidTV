package com.kt.apps.media.mobile.models

import com.google.android.exoplayer2.PlaybackException

class PlaybackThrowable(val code: Int, val error: PlaybackException ): Throwable(message = "PlaybackError")
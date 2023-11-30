package com.kt.apps.media.mobile.models

import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.video.VideoSize

data class ChannelInfo(
    val title: String,
    val duration: String,
    val videoSize: VideoSize,
    val audioFormat: Format?,
    val videoFormat: Format?
)
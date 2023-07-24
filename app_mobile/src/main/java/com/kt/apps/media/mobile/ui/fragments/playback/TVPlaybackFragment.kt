package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.channels.BasePlaybackFragment
import com.kt.apps.media.mobile.viewmodels.BasePlaybackControlViewModel
import com.kt.apps.media.mobile.viewmodels.TVPlaybackControlViewModel


class TVPlaybackFragment : BasePlaybackFragment() {
    private val _playbackViewModel by lazy {
        TVPlaybackControlViewModel(ViewModelProvider(requireActivity(), factory))
    }
    override val playbackViewModel: BasePlaybackControlViewModel
        get() = _playbackViewModel
}
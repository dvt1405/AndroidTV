package com.kt.apps.media.mobile.ui.fragments.playback

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.viewmodels.BasePlaybackControlViewModel
import com.kt.apps.media.mobile.viewmodels.TVPlaybackControlViewModel


class TVPlaybackFragment : BasePlaybackFragment() {
    private val _playbackViewModel by lazy {
        TVPlaybackControlViewModel(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackControlViewModel
        get() = _playbackViewModel
}

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        BasePlaybackControlViewModel(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackControlViewModel
        get() = _playbackViewModel

}
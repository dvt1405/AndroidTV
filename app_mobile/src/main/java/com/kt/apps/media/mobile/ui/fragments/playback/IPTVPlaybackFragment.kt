package com.kt.apps.media.mobile.ui.fragments.playback

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.media.mobile.models.StreamLinkData
import com.kt.apps.media.mobile.utils.alignParent
import com.kt.apps.media.mobile.utils.matchParentWidth
import com.kt.apps.media.mobile.utils.repeatLaunchsOnLifeCycle
import com.kt.apps.media.mobile.utils.safeLet
import com.kt.apps.media.mobile.viewmodels.BasePlaybackInteractor
import com.kt.apps.media.mobile.viewmodels.IPTVPlaybackInteractor
import kotlinx.coroutines.flow.collectLatest

class IPTVPlaybackFragment: BasePlaybackFragment() {

    private val _playbackViewModel by lazy {
        IPTVPlaybackInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    override val playbackViewModel: BasePlaybackInteractor
        get() = _playbackViewModel

    override fun provideMinimalLayout(): ConstraintSet? {
        return safeLet(binding.motionLayout, binding.exoPlayer, binding.minimalLayout, binding.channelList) {
                mainLayout, exoplayer,  minimal, list ->
            ConstraintSet().apply {
                clone(mainLayout)
                arrayListOf(exoplayer.id, minimal.id, list.id).forEach {
                    clear(it)
                }

                setVisibility(list.id, View.GONE)
                matchParentWidth(list.id)
                matchParentWidth(minimal.id)
                matchParentWidth(exoplayer.id)
                constrainHeight(minimal.id, ConstraintSet.WRAP_CONTENT)
                connect(exoplayer.id, ConstraintSet.BOTTOM, minimal.id, ConstraintSet.TOP)
                alignParent(minimal.id, ConstraintSet.BOTTOM)
                alignParent(exoplayer.id, ConstraintSet.TOP)
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        super.initAction(savedInstanceState)
        repeatLaunchsOnLifeCycle(Lifecycle.State.STARTED, listOf {
            _playbackViewModel.relatedItems
                .collectLatest {
                    binding.channelList?.reloadAllData(it)
                }
        })
    }

    override suspend fun playVideo(data: StreamLinkData) {
        super.playVideo(data)

        (data as? StreamLinkData.ExtensionStreamLinkData)?.run {
            _playbackViewModel.loadChannelConfig(this.category)
        }
    }


    companion object {
        const val screenName = "IPTVPlaybackFragment"
    }
}
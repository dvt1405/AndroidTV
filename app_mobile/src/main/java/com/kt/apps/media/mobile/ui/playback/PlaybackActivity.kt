package com.kt.apps.media.mobile.ui.playback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityPlaybackBinding
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.TVChannelAdapter
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class PlaybackActivity : BaseActivity<ActivityPlaybackBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile

    private val adapter by lazy {
        TVChannelAdapter()
    }

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    private val exoControllerRecyclerView: RecyclerView by lazy {
        binding.exoPlayer.findViewById(R.id.controller_content)
    }

    private val titleVideoView: TextView by lazy {
        binding.exoPlayer.findViewById(R.id.title)
    }

    private val exoControllerNextView: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_next)
    }

    private val exoControllerPreView: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_prev)
    }

    private val exoControllerBackgroundView: View by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_controls_background)
    }

    private val exoControllerExtraControlView: View by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_extra_controls_scroll_view)
    }

    private val  exoSettingView: View by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_settings)
    }


    private var currentPlayingItem: TVChannelLinkStream? = null

    private val mPlayerListener by lazy {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    binding.progressContainerLayout
                        .progressDialog
                        .gone()
                } else {
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
            }
        }
    }

    override val layoutRes: Int
        get() = R.layout.activity_playback

    override fun initView(savedInstanceState: Bundle?) {
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        exoControllerRecyclerView.adapter = adapter
        adapter.onItemRecyclerViewCLickListener = { item, position ->
            tvChannelViewModel.getLinkStreamForChannel(item)

        }
        if (savedInstanceState == null) {
            tvChannelViewModel.getListTVChannel(false)
        }

        binding.exoPlayer.showController()
        exoSettingView.visibility = View.GONE
        exoControllerRecyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_DRAGGING) {
                    binding.exoPlayer.controllerAutoShow = true
                    binding.exoPlayer.showController()
                    binding.exoPlayer.controllerHideOnTouch = false
                } else if (newState == SCROLL_STATE_IDLE) {
                    binding.exoPlayer.controllerHideOnTouch = true

                }
            }
        })
        var currentAlpha = 0f
        exoControllerBackgroundView.viewTreeObserver.addOnDrawListener {
            if (currentAlpha != exoControllerBackgroundView.alpha) {
                exoControllerRecyclerView.alpha = exoControllerBackgroundView.alpha
            }
            currentAlpha = exoControllerBackgroundView.alpha
        }

    }

    override fun initAction(savedInstanceState: Bundle?) {
        intent?.data?.let {
            playContentByDeepLink(it)
        }
        exoControllerPreView.setOnClickListener {
            val list = adapter.listItem.ifEmpty {
                return@setOnClickListener
            }

            val index = list.indexOf(currentPlayingItem?.channel)
            Logger.d(this@PlaybackActivity, tag = "ControllerPrev", message = "CurrentItemPosition = $index")
            tvChannelViewModel.getLinkStreamForChannel(list[index - 1])
        }

        exoControllerNextView.setOnClickListener {
            val list = adapter.listItem.ifEmpty {
                return@setOnClickListener
            }

            val index = list.indexOf(currentPlayingItem?.channel)
            Logger.d(this@PlaybackActivity, tag = "ControllerNext", message = "CurrentItemPosition = $index")
            tvChannelViewModel.getLinkStreamForChannel(list[index + 1])

        }
        when (intent?.getParcelableExtra<Type>(Constants.EXTRA_PLAYBACK_TYPE)) {
            Type.TV, Type.RADIO -> {
                tvChannelViewModel.getListTVChannel(false)
                val data = intent.extras!!.getParcelable<TVChannelLinkStream>(Constants.EXTRA_TV_CHANNEL)!!
                playVideo(data)

            }

            else -> {

            }
        }
        tvChannelViewModel.tvChannelLiveData.observe(this) {
            when (it) {
                is DataState.Success -> {
                    adapter.onRefresh(it.data)
                }

                is DataState.Loading -> {

                }

                else -> {

                }
            }

        }

        tvChannelViewModel.tvWithLinkStreamLiveData.observe(this) {
            when (it) {
                is DataState.Success -> {
                    val data = it.data
                    playVideo(data)
                    binding.progressContainerLayout
                        .progressDialog
                        .gone()
                }

                is DataState.Loading -> {
                    binding.progressContainerLayout
                        .progressDialog
                        .visible()
                }

                else -> {

                }
            }

        }
    }

    private fun playVideo(data: TVChannelLinkStream) {
        currentPlayingItem = data
        exoPlayerManager.playVideo(
            linkStreams = data.linkStream.map {
                LinkStream(it, data.channel.tvChannelWebDetailPage, data.channel.tvChannelWebDetailPage)
            },
            isHls = data.channel.isHls,
            itemMetaData = data.channel.getMapData()
        )
        titleVideoView.text = data.channel.tvChannelName
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            playContentByDeepLink(it)
        }
    }

    private fun playContentByDeepLink(deepLink: Uri) {
        when {
            deepLink.host.equals(Constants.HOST_TV) || deepLink.host.equals(Constants.HOST_RADIO) -> {
                tvChannelViewModel.playTvByDeepLinks(deepLink)
                intent?.data = null
            }

            else -> {
//                startActivity(Intent(this, MainActivity::class.java)
//                    .apply {
//                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    }
//                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        exoPlayerManager.exoPlayer?.play()
    }

    override fun onPause() {
        binding.exoPlayer.player = null
        exoPlayerManager.exoPlayer?.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        exoPlayerManager.detach(null)
    }

    @Parcelize
    enum class Type : Parcelable {
        TV, FOOTBALL, RADIO
    }

}
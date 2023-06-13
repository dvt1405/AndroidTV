package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.exoplayer2.PlaybackException
import com.kt.apps.core.R
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logPlaybackRetryGetStreamLink
import com.kt.apps.core.logging.logPlaybackRetryPlayVideo
import com.kt.apps.core.logging.logPlaybackShowError
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.main.MainActivity
import dagger.android.AndroidInjector
import javax.inject.Inject
import kotlin.math.max

/** Handles video playback with media controls. */
class TVPlaybackVideoFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val retryTimes by lazy {
        mutableMapOf<String, Int>()
    }

    override fun onHandlePlayerError(error: PlaybackException) {
        super.onHandlePlayerError(error)
        Logger.e(this, exception = error.cause ?: Throwable("Error playback"))
        val retriedTimes = try {
            retryTimes[tvChannelViewModel.lastWatchedChannel?.channel?.channelId] ?: 0
        } catch (e: Exception) {
            0
        }

        when {
            retriedTimes > MAX_RETRY_TIME || (tvChannelViewModel.lastWatchedChannel?.linkStream?.size ?: 0) == 0 -> {
                notifyPlaybackError(error)
            }

            (tvChannelViewModel.lastWatchedChannel?.linkStream?.size ?: 0) > 1 -> {
                val newStreamList = tvChannelViewModel.lastWatchedChannel!!.linkStream.subList(
                    1,
                    tvChannelViewModel.lastWatchedChannel!!.linkStream.size
                )
                val newChannelWithLink = TVChannelLinkStream(
                    tvChannelViewModel.lastWatchedChannel!!.channel,
                    newStreamList
                )
                tvChannelViewModel.markLastWatchedChannel(newChannelWithLink)
                playVideo(newChannelWithLink)
                actionLogger.logPlaybackRetryPlayVideo(
                    error,
                    tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "Unknown",
                    "link" to newStreamList.first()
                )
                retryTimes[tvChannelViewModel.lastWatchedChannel?.channel!!.channelId] = retriedTimes + 1
            }

            else -> {
                tvChannelViewModel.retryGetLastWatchedChannel()
                actionLogger.logPlaybackRetryGetStreamLink(
                    error,
                    tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "Unknown"
                )
                retryTimes[tvChannelViewModel.lastWatchedChannel?.channel!!.channelId] = retriedTimes + 1
            }
        }
    }

    private fun notifyPlaybackError(error: PlaybackException) {
        showErrorDialog(
            content = "Kênh ${tvChannelViewModel.lastWatchedChannel?.channel?.tvChannelName ?: "TV"} " +
                    "hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí: " +
                    "${error.message} ${error.errorCode}"
        )
        val channel = tvChannelViewModel.lastWatchedChannel?.channel ?: return
        retryTimes[channel.channelId] = 0

        actionLogger.logPlaybackShowError(
            error,
            channel.tvChannelName
        )
    }

    override fun onError(errorCode: Int, errorMessage: CharSequence?) {
        super.onError(errorCode, errorMessage)
    }

    private var mCurrentSelectedChannel: TVChannel? = null
    private var mSelectedPosition: Int = 0
    private var mPlayingPosition: Int = 0
    override val numOfRowColumns: Int
        get() = 5

    private fun setupRowAdapter(tvChannelList: List<TVChannel>) {
        mSelectedPosition = mCurrentSelectedChannel?.let {
            val lastChannel = tvChannelList.findLast {
                it.channelId == mCurrentSelectedChannel!!.channelId
            }
            tvChannelList.lastIndexOf(lastChannel)
        } ?: 0
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = TVChannelPresenterSelector(requireActivity())
        val mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        mAdapter.addAll(0, tvChannelList)
        this.mAdapter = mAdapter
        updateAdapter()
    }

    override fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        mGridViewHolder?.gridView?.setSelectedPositionSmooth(position)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (tvChannelViewModel.tvChannelLiveData.value !is DataState.Success) {
            tvChannelViewModel.getListTVChannel(false)
        }

        val tvChannel = arguments?.getParcelable<TVChannelLinkStream?>(PlaybackActivity.EXTRA_TV_CHANNEL)
        tvChannelViewModel.markLastWatchedChannel(tvChannel)
        tvChannel?.let {
            mCurrentSelectedChannel = it.channel
            setBackgroundByStreamingType(it)
            playVideo(tvChannel)
            tvChannelViewModel.markLastWatchedChannel(it)
        }
        onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
            (mAdapter as ArrayObjectAdapter).indexOf(item)
                .takeIf {
                    it > -1
                }?.let {
                    mSelectedPosition = it
                }
        }
        tvChannelViewModel.tvWithLinkStreamLiveData.observe(viewLifecycleOwner) {
            streamingByDataState(it)
        }

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            loadChannelListByDataState(it)
        }
        tvChannelViewModel.programmeForChannelLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                if (
                    tvChannelViewModel.lastWatchedChannel
                        ?.channel
                        ?.channelId
                        ?.removeAllSpecialChars()
                        ?.removePrefix("viechannel")
                    == it.data.channel
                ) {
                    showInfo(it.data)
                }
            }
        }
    }

    private fun setBackgroundByStreamingType(it: TVChannelLinkStream) {
        if (it.channel.isRadio) {
            getBackgroundView()?.setBackgroundResource(R.drawable.bg_radio_playing)
        } else {
            getBackgroundView()?.background = ColorDrawable(Color.TRANSPARENT)
        }
    }

    private fun loadChannelListByDataState(dataState: DataState<List<TVChannel>>) {
        when (dataState) {
            is DataState.Success -> {
                setupRowAdapter(dataState.data)
                if (mPlayingPosition <= 0 && mCurrentSelectedChannel != null) {
                    mPlayingPosition = dataState.data.indexOfLast {
                        it.channelId == mCurrentSelectedChannel!!.channelId
                    }.takeIf {
                        it >= 0
                    } ?: 0
                }
            }
            is DataState.Error -> {

            }
            is DataState.Loading -> {

            }
            else -> {

            }
        }
    }

    private fun streamingByDataState(dataState: DataState<TVChannelLinkStream>?) {
        when (dataState) {
            is DataState.Success -> {
                mCurrentSelectedChannel = dataState.data.channel
                progressBarManager.hide()
                val tvChannel = dataState.data
                if (tvChannel.channel.isRadio) {
                    getBackgroundView()?.setBackgroundResource(R.drawable.bg_radio_playing)
                } else {
                    getBackgroundView()?.background = ColorDrawable(Color.TRANSPARENT)
                }
                playVideo(tvChannel)
                Logger.d(this, message = "Play media source")
            }

            is DataState.Loading -> {
                progressBarManager.show()
            }

            is DataState.Error -> {
                progressBarManager.hide()

                showErrorDialog(content = dataState.throwable.message,
                    onSuccessListener = {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    })
            }

            else -> {
                progressBarManager.hide()
            }
        }
    }

    private fun showInfo(tvChannel: TVScheduler.Programme) {
        Logger.d(this@TVPlaybackVideoFragment, "ChannelInfo", message = "$tvChannel")
        val channelTitle = tvChannel.title.takeIf {
            it.trim().isNotBlank()
        }?.trim() ?: ""
        prepare(
            tvChannel.channel.uppercase() + if (channelTitle.isNotBlank()) {
                " - $channelTitle"
            } else {
                ""
            },
            tvChannel.description.takeIf {
                it.isNotBlank()
            }?.trim(),
            isLive = true,
            showProgressManager = false
        )
    }

    private fun playVideo(tvChannel: TVChannelLinkStream) {
        playVideo(
            title = tvChannel.channel.tvChannelName,
            null,
            referer = if (tvChannel.channel.referer.isEmpty()) {
                tvChannel.linkStream.first()
            } else {
                tvChannel.channel.referer
            },
            linkStream = tvChannel.linkStream,
            true,
            isHls = tvChannel.channel.isHls
        )
        tvChannelViewModel.loadProgramForChannel(tvChannel.channel)
        Logger.d(this, message = "PlayVideo: $tvChannel")
        if (tvChannelViewModel.tvChannelLiveData.value is DataState.Success) {
            val listChannel = (tvChannelViewModel.tvChannelLiveData.value as DataState.Success<List<TVChannel>>).data
            mPlayingPosition = listChannel.indexOfLast {
                it.channelId == mCurrentSelectedChannel!!.channelId
            }.takeIf {
                it >= 0
            } ?: 0
        }

    }

    override fun onDetach() {
        progressBarManager.hide()
        Logger.d(this, message = "onDetach")
        super.onDetach()
    }

    override fun onKeyCodeChannelDown() {
        super.onKeyCodeChannelDown()
        mPlayingPosition = max(0, mPlayingPosition) - 1
        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelDown: $mPlayingPosition")
        if (mPlayingPosition > 0) {
            val item = mAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    override fun onKeyCodeChannelUp() {
        super.onKeyCodeChannelUp()
        mPlayingPosition = max(0, mPlayingPosition) + 1

        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelUp: $mPlayingPosition")
        val maxItemCount = mGridViewHolder?.gridView?.adapter?.itemCount ?: 0
        if (mPlayingPosition <= maxItemCount - 1) {
            val item = mAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    companion object {
        fun newInstance(type: PlaybackActivity.Type, tvChannelLinkStream: TVChannelLinkStream) = TVPlaybackVideoFragment().apply {
            val args = Bundle()
            args.putParcelable(PlaybackActivity.EXTRA_PLAYBACK_TYPE, type)
            args.putParcelable(PlaybackActivity.EXTRA_TV_CHANNEL, tvChannelLinkStream)
            this.arguments = args
        }
    }
}
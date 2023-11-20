package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.kt.apps.core.ErrorCode
import com.kt.apps.core.R
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.PresenterSelector
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logPlaybackRetryGetStreamLink
import com.kt.apps.core.logging.logPlaybackRetryPlayVideo
import com.kt.apps.core.logging.logPlaybackShowError
import com.kt.apps.core.tv.datasource.impl.MainTVDataSource
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.favorite.FavoriteViewModel
import dagger.android.AndroidInjector
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.max

/** Handles video playback with media controls. */
class TVPlaybackVideoFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val _favoriteViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FavoriteViewModel::class.java]
    }
    private val retryTimes by lazy {
        mutableMapOf<String, Int>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteViewModel = _favoriteViewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        tvChannelViewModel.programmeForChannelLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                val lastWatchedChannel = tvChannelViewModel.lastWatchedChannel?.channel ?: return@observe
                showInfo(
                    programme = it.data,
                    lastWatchedChannel
                )
                lastWatchedChannel.currentProgramme = it.data
                updateProgress(exoPlayerManager.exoPlayer)
            } else if (it is DataState.Update) {
                val lastWatchedChannel = tvChannelViewModel.lastWatchedChannel?.channel ?: return@observe
                updateVideoInfo(
                    lastWatchedChannel.tvChannelName,
                    buildVideoDescription(it.data),
                    true
                )
                lastWatchedChannel.currentProgramme = it.data
                updateProgress(exoPlayerManager.exoPlayer)
            }
        }
        return rootView
    }
    override fun onFavoriteVideoClicked(isFavorite: Boolean) {
        super.onFavoriteVideoClicked(isFavorite)
        tvChannelViewModel.lastWatchedChannel?.channel?.let {
            if (isFavorite) {
                _favoriteViewModel.saveTVChannel(it)
            } else {
                _favoriteViewModel.deleteFavoriteTvChannel(it)
            }
        }
    }

    override fun getSearchFilter(): String {
        return SearchForText.FILTER_ONLY_TV_CHANNEL
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
            retriedTimes > MAX_RETRY_TIME || (tvChannelViewModel.lastWatchedChannel?.linkReadyToStream?.size ?: 0) == 0 -> {
                if (tvChannelViewModel.lastWatchedChannel?.linkStream?.filter {
                        it.type == MainTVDataSource.TVChannelUrlType.WEB_PAGE.value
                    }.isNullOrEmpty()) {
                    notifyPlaybackError(error)
                } else {
                    retryTimes[tvChannelViewModel.lastWatchedChannel?.channel!!.channelId] = 0
                    tvChannelViewModel.getLinkStreamForChannel(
                        tvChannelViewModel.lastWatchedChannel!!.channel,
                        true
                    )
                }
            }

            (tvChannelViewModel.lastWatchedChannel?.linkReadyToStream?.size ?: 0) > 1 -> {
                val newStreamList = tvChannelViewModel.lastWatchedChannel!!.linkReadyToStream.subList(
                    1,
                    tvChannelViewModel.lastWatchedChannel!!.linkReadyToStream.size
                )
                val newChannelWithLink = TVChannelLinkStream(
                    tvChannelViewModel.lastWatchedChannel!!.channel,
                    newStreamList
                )
                tvChannelViewModel.markLastWatchedChannel(newChannelWithLink)
                playVideo(newChannelWithLink, false)
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
        showErrorDialogWithErrorCode(errorCode = error.errorCode)
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
    override val listProgramForChannelLiveData: LiveData<DataState<List<TVScheduler.Programme>>>
        get() = tvChannelViewModel.listProgramForChannel

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
        tvChannel?.let {
            mCurrentSelectedChannel = it.channel
            setBackgroundByStreamingType(it)
//            tvChannelViewModel.loadProgramForChannel(it.channel)
            tvChannelViewModel.getListProgramForChannel(it.channel)
            tvChannelViewModel.markLastWatchedChannel(it)
            playVideo(tvChannel)
        }
        onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            onRemoveProgramSchedule()
            tvChannelViewModel.markLastWatchedChannel(item as TVChannel)
            getLinkStreamAndProgramForChannel(item)
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
            val isRadio = tvChannel?.channel?.isRadio ?: false
            loadChannelListByDataState(it, isRadio)
        }
    }

    private fun onRemoveProgramSchedule() {
        try {
            childFragmentManager.findFragmentById(R.id.container_program)?.let {
                childFragmentManager.beginTransaction()
                    .remove(it)
                    .commitNow()
            }
        } catch (_: Exception) {
        }
    }

    override fun onCreateProgramScheduleFragment(): Fragment? {
        return FragmentProgramSchedule.newInstance()
    }

    override fun onHideProgramSchedule() {
        super.onHideProgramSchedule()
    }

    override fun updateProgress(player: Player?) {
        val currentProgram = getCurrentProgram()
        currentProgram?.let {
            if (it.start.isNotEmpty() && it.stop.isNotEmpty()) {
                val startTime = hourMinuteFormatter.format(it.startTimeMilli())
                val endTime = hourMinuteFormatter.format(it.endTimeMilli())
                playbackLiveProgramDuration?.text = "$startTime - $endTime"
                playbackLiveProgramDuration?.visible()
                seekBar?.max = (it.endTimeMilli() - it.startTimeMilli()).toInt()
                seekBar?.progress = (System.currentTimeMillis() - it.startTimeMilli()).toInt()
            } else {
                playbackLiveProgramDuration?.gone()
                seekBar?.max = 1
                seekBar?.progress = 1
            }
            seekBar?.isSeekAble = false
            contentPositionView?.gone()
            contentDurationView?.gone()
        } ?: run {
            seekBar?.max = 1
            seekBar?.progress = 1
            playbackLiveProgramDuration?.text = tvChannelViewModel.lastWatchedChannel?.channel?.tvGroup
            playbackLiveProgramDuration?.visible()
            contentDurationView?.gone()
            contentPositionView?.gone()
        }
    }
    private fun getLinkStreamAndProgramForChannel(item: TVChannel) {
//        tvChannelViewModel.loadProgramForChannel(item)
        tvChannelViewModel.getListProgramForChannel(item)
        tvChannelViewModel.getLinkStreamForChannel(item)
    }

    private fun setBackgroundByStreamingType(it: TVChannelLinkStream) {
        if (it.channel.isRadio) {
            getBackgroundView()?.setBackgroundResource(R.drawable.bg_radio_playing)
        } else {
            getBackgroundView()?.background = ColorDrawable(Color.TRANSPARENT)
        }
    }

    private fun loadChannelListByDataState(dataState: DataState<List<TVChannel>>, isRadio: Boolean) {
        when (dataState) {
            is DataState.Success -> {
                if (isRadio) {
                    setupRowAdapter(dataState.data.filter {
                        it.isRadio
                    })
                } else {
                    setupRowAdapter(dataState.data.filter {
                        !it.isRadio
                    })
                }
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
                playVideo(tvChannel, false)
                Logger.d(this, message = "Play media source")
            }

            is DataState.Loading -> {
                if (!progressManager.isShowing) {
                    progressBarManager.show()
                }
            }

            is DataState.Error -> {
                progressBarManager.hide()
                if (dataState.throwable is GetChannelLinkStreamById.ChannelNotFoundThrowable) {
                    showErrorDialogWithErrorCode(ErrorCode.GET_STREAM_LINK_ERROR, dataState.throwable.message) {
                        try {
                            startActivity(Intent().apply {
                                this.data = Uri.parse("xemtv://tv/dashboard")
                                this.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            activity?.finish()
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    showErrorDialogWithErrorCode(ErrorCode.GET_STREAM_LINK_ERROR)
                }
                tvChannelViewModel.lastWatchedChannel?.let {
                    retryTimes[it.channel.channelId] = 0
                }
            }

            else -> {
                progressBarManager.hide()
            }
        }
    }

    private fun showInfo(programme: TVScheduler.Programme, channel: TVChannel) {
        prepare(
            channel.tvChannelName,
            buildVideoDescription(programme),
            isLive = true,
            showProgressManager = false
        )
    }

    private fun buildVideoDescription(programme: TVScheduler.Programme): String {
        val description = programme.getProgramDescription()
        Logger.d(this@TVPlaybackVideoFragment, "ChannelInfo", message = "$programme,\n" +
                "description: $description" +
                "")

        val channelTitle = programme.title.takeIf {
            it.trim().isNotBlank()
        }?.trim() ?: ""

        return if (channelTitle.isEmpty()) {
            description
        } else {
            channelTitle + if (description.isEmpty()) {
                ""
            } else {
                " • $description"
            }
        }
    }

    private fun playVideo(tvChannelLinkStream: TVChannelLinkStream, showVideoInfo: Boolean = true) {
        playVideo(
            linkStreams = tvChannelLinkStream.inputExoPlayerLink,
            playItemMetaData = tvChannelLinkStream.channel.getMapData(),
            isHls = tvChannelLinkStream.channel.isHls,
            headers = null,
            isLive = true,
            forceShowVideoInfoContainer = showVideoInfo
        )
        Logger.d(this, message = "PlayVideo: $tvChannelLinkStream")
        if (tvChannelViewModel.tvChannelLiveData.value is DataState.Success) {
            val listChannel = (tvChannelViewModel.tvChannelLiveData.value as DataState.Success<List<TVChannel>>).data
            mPlayingPosition = listChannel.indexOfLast {
                it.channelId == mCurrentSelectedChannel?.channelId
            }.takeIf {
                it >= 0
            } ?: 0
        }

    }

    override fun getCurrentProgram(): TVScheduler.Programme? {
        return tvChannelViewModel.programmeForChannelLiveData.value?.let {
            when (it) {
                is DataState.Success -> {
                    it.data
                }

                is DataState.Update -> {
                    it.data
                }

                else -> {
                    null
                }
            }
        }
    }

    override fun onRefreshProgram() {
        super.onRefreshProgram()
        tvChannelViewModel.lastWatchedChannel?.let {
            if (System.currentTimeMillis() - tvChannelViewModel.lastGetProgramme >= 1 * 60 * 1000) {
                tvChannelViewModel.loadProgramForChannel(it.channel, true)
            }
        }
    }

    override fun onDetach() {
        progressBarManager.hide()
        Logger.d(this, message = "onDetach")
        super.onDetach()
    }

    override fun onKeyCodeChannelDown() {
        super.onKeyCodeChannelDown()
        val nextPosition = max(0, mPlayingPosition) - 1
        if (nextPosition <= -1) return
        mPlayingPosition = nextPosition
        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelDown: $mPlayingPosition")
        val maxItemCount = mGridViewHolder?.gridView?.adapter?.itemCount ?: 0
        if (mPlayingPosition <= maxItemCount - 1) {
            val item = mAdapter?.get(mPlayingPosition) as? TVChannel ?: return
            getLinkStreamAndProgramForChannel(item)
        } else {
            mPlayingPosition = maxItemCount - 1
        }
    }

    override fun onKeyCodeChannelUp() {
        super.onKeyCodeChannelUp()
        mPlayingPosition = max(0, mPlayingPosition) + 1

        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelUp: $mPlayingPosition")
        val maxItemCount = mGridViewHolder?.gridView?.adapter?.itemCount ?: 0
        if (mPlayingPosition <= maxItemCount - 1) {
            val item = mAdapter?.get(mPlayingPosition) as? TVChannel ?: return
            getLinkStreamAndProgramForChannel(item)
        } else {
            mPlayingPosition = mGridViewHolder?.gridView?.selectedPosition ?: 0
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    companion object {
        val hourMinuteFormatter = SimpleDateFormat("HH:mm")
        fun newInstance(type: PlaybackActivity.Type, tvChannelLinkStream: TVChannelLinkStream) = TVPlaybackVideoFragment().apply {
            val args = Bundle()
            args.putParcelable(PlaybackActivity.EXTRA_PLAYBACK_TYPE, type)
            args.putParcelable(PlaybackActivity.EXTRA_TV_CHANNEL, tvChannelLinkStream)
            this.arguments = args
        }
    }
}
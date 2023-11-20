package com.kt.apps.media.xemtv.ui.extensions

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Util
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logStreamingTV
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.core.utils.isShortLink
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.favorite.FavoriteViewModel
import com.kt.apps.media.xemtv.ui.playback.FragmentProgramSchedule
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class FragmentExtensionsPlayback : BasePlaybackFragment() {
    override val numOfRowColumns: Int
        get() = 5
    override val listProgramForChannelLiveData: LiveData<DataState<List<TVScheduler.Programme>>>
        get() = extensionsViewModel.listProgramForChannel

    private var itemToPlay: ExtensionsChannel? = null
    private val listCurrentItem by lazy {
        mutableListOf<ExtensionsChannel>()
    }
    private val retryTimes by lazy {
        mutableMapOf<String, Int>()
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }
    private val _favoriteViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FavoriteViewModel::class.java]
    }

    private val extension: ExtensionsConfig by lazy {
        requireArguments().getParcelable(EXTRA_EXTENSION_ID)!!
    }
    override var allowDpadUpToOpenSearch = false
    override fun getSearchFilter(): String {
        return extension.sourceUrl
    }

    override fun getSearchHint(): String? {
        return extension.sourceName
    }

    override fun onFavoriteVideoClicked(isFavorite: Boolean) {
        if (isFavorite) {
            itemToPlay?.let { _favoriteViewModel.saveFavorite(it) }
        } else {
            itemToPlay?.let { _favoriteViewModel.deleteFavorite(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemToPlay = requireArguments()[EXTRA_TV_CHANNEL] as ExtensionsChannel?
        favoriteViewModel = _favoriteViewModel
    }

    override fun onHandlePlayerError(error: PlaybackException) {
        super.onHandlePlayerError(error)
    }

    override fun onPlayerPlaybackStateChanged(playbackState: Int) {
        super.onPlayerPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_READY -> {
                itemToPlay?.let {
                    actionLogger.logStreamingTV(
                        it.tvChannelName,
                        "channelLink" to it.tvStreamLink
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemToPlay?.let {
            Logger.d(this, message = "$it")
            playVideo(it)
        }
        Logger.e(this@FragmentExtensionsPlayback, message = "id = $extension")
        extensionsViewModel.loadChannelForConfig(extension.sourceUrl).observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                refreshListRelatedItem(it.data)
                onItemClickedListener = OnItemViewClickedListener { _, item, rowViewHolder, row ->
                    itemToPlay?.let {
                        retryTimes[it.channelId] = 0
                    }
                    itemToPlay = item as? ExtensionsChannel
                    refreshListRelatedItem(it.data)
                    itemToPlay?.let { it1 -> playVideo(it1) }
                }
            }
        }

        extensionsViewModel.programmeForChannelLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                if (itemToPlay?.channelId == it.data.channel) {
                    itemToPlay?.currentProgramme = it.data
                    itemToPlay?.let { it1 -> showInfo(it1, false) }
                }
            }
        }

        extensionsViewModel.historyItem.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                val historyData = it.data
                val timeString = Util.getStringForTime(
                    formatBuilder,
                    formatter,
                    historyData.currentPosition
                )
                val spannableString = SpannableString("Bạn đang xem ${it.data.displayName} tại ${timeString}." +
                        "\nBạn có muốn tiếp tục không?")
                val timeIndex = spannableString.indexOf(timeString)
                val titleIndex = spannableString.indexOf(it.data.displayName)
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), com.kt.apps.resources.R.color.color_text_highlight)),
                    timeIndex,
                    timeIndex + timeString.length,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                )
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), com.kt.apps.resources.R.color.color_text_highlight)),
                    titleIndex,
                    titleIndex + it.data.displayName.length,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                )


                if (itemToPlay?.channelId == historyData.itemId) {
                    AlertDialog.Builder(
                        ContextThemeWrapper(
                            requireActivity(),
                            androidx.appcompat.R.style.Theme_AppCompat
                        ), com.kt.apps.resources.R.style.AlertDialogTheme
                    )
                        .setTitle(it.data.category)
                        .setMessage(spannableString)
                        .setPositiveButton("Có") { dialog, which ->
                            showAllOverlayElements()
                            exoPlayerManager.exoPlayer?.seekTo(historyData.currentPosition)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Xem lại từ đầu") { dialog, which ->
                            dialog.dismiss()
                        }
                        .create()
                        .apply {
                            this.setOnShowListener {
                                this.findViewById<View>(android.R.id.button1)?.requestFocus()
                            }
                        }
                        .show()

                }
            }
        }


    }

    private fun refreshListRelatedItem(data: List<ExtensionsChannel>) {
        listCurrentItem.clear()
        itemToPlay?.let { channel ->
            listCurrentItem.addAll(
                data.filter {
                    it.tvGroup == channel.tvGroup
                }
            )
            listCurrentItem.addAll(data.filter {
                it.tvGroup != channel.tvGroup
            })
        } ?: listCurrentItem.addAll(data)
        setupRowAdapter(listCurrentItem, TVChannelPresenterSelector(requireActivity()))
    }

    override fun onError(errorCode: Int, errorMessage: CharSequence?) {
        super.onError(errorCode, errorMessage)
        Logger.e(this, message = "errorCode: $errorCode, errorMessage: $errorMessage")
        val retriedTimes = retryTimes[itemToPlay!!.channelId] ?: 0
        val useMainSource = if (itemToPlay != null) {
            if (itemToPlay?.catchupSource.isNullOrBlank()) {
                true
            } else {
                retriedTimes < 2
            }
        } else {
            return
        }

        when {
            retriedTimes > MAX_RETRY_TIME -> {
                showErrorDialogWithErrorCode(errorCode)
                retryTimes[itemToPlay!!.channelId] = 0
            }

            useMainSource -> {
                itemToPlay?.let {
                    playVideo(it, useCatchup = false, refreshProgramForChannel = false)
                }
                retryTimes[itemToPlay!!.channelId] = retriedTimes + 1
            }

            !itemToPlay?.catchupSource.isNullOrBlank() -> {
                itemToPlay?.let {
                    playVideo(it, useCatchup = true, refreshProgramForChannel = false)
                }
                retryTimes[itemToPlay!!.channelId] = retriedTimes + 1
            }
        }
    }

    private var lastExpandUrlTask: Disposable? = null
    private val disposable by lazy {
        CompositeDisposable()
    }
    private fun playVideo(
        extensionsChannel: ExtensionsChannel,
        useCatchup: Boolean = false,
        refreshProgramForChannel: Boolean = true
    ) {
        if (!progressManager.isShowing) {
            progressManager.show()
        }
        if (refreshProgramForChannel) {
            extensionsViewModel.getListProgramForChannel(extensionsChannel, extension.type)
        }
        lastExpandUrlTask?.let { disposable.remove(it) }
        disposable.clear()
        val linkToPlay = if (!useCatchup) {
            extensionsChannel.tvStreamLink
        } else {
            extensionsChannel.catchupSource
        }

        Logger.d(
            this,
            "PlayVideo",
            "$extensionsChannel,\t" +
                    "useCatchup: $useCatchup," +
                    "isHls: ${
                        linkToPlay.contains("m3u8") ||
                                linkToPlay.isShortLink()
                    }" +
                    ""
        )

        Logger.d(
            this,
            "LinkPlayVideo",
            linkToPlay
        )

        if (refreshProgramForChannel) {
            showInfo(extensionsChannel)
        }

        if (linkToPlay.isShortLink()) {
            lastExpandUrlTask = Observable.fromCallable {
                linkToPlay.expandUrl()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ realUrl ->
                    if (isDetached) return@subscribe
                    playWhenReady(extensionsChannel, realUrl)
                }, {
                    if (isDetached) return@subscribe
                    playWhenReady(extensionsChannel, linkToPlay)
                })
            disposable.add(lastExpandUrlTask!!)

        } else if (linkToPlay.contains(".m3u8")) {
            playWhenReady(extensionsChannel, linkToPlay)
        } else {
            disposable.add(Observable.fromCallable {
                linkToPlay.expandUrl()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ realUrl ->
                    if (isDetached) return@subscribe
                    playWhenReady(extensionsChannel, realUrl)
                }, {
                    if (isDetached) return@subscribe
                    playWhenReady(extensionsChannel, linkToPlay)
                }))

        }
    }

    private fun FragmentExtensionsPlayback.playWhenReady(
        extensionsChannel: ExtensionsChannel,
        linkToPlay: String
    ) {
        extensionsViewModel.getHistoryForItem(extensionsChannel, linkToPlay)
        playVideo(
            linkStreams = listOf(
                LinkStream(
                    linkToPlay,
                    extensionsChannel.referer,
                    streamId = extensionsChannel.channelId,
                    isHls = extensionsChannel.isHls || linkToPlay.contains("m3u8")
                )
            ),
            playItemMetaData = extensionsChannel.getMapData(),
            isHls = extensionsChannel.isHls || linkToPlay.contains("m3u8"),
            headers = extensionsChannel.props,
            isLive = extension.type == ExtensionsConfig.Type.FOOTBALL,
            forceShowVideoInfoContainer = false
        )
    }
    override fun onCreateProgramScheduleFragment(): Fragment? {
        return FragmentProgramSchedule.newInstance()
    }

    override fun onPause() {
        extensionsViewModel.clearHistoryDataState()
        super.onPause()
    }

    private fun showInfo(
        tvChannel: ExtensionsChannel,
        showProgressManager: Boolean = true
    ) {
        Logger.d(this@FragmentExtensionsPlayback, "ChannelInfo", message = "$tvChannel")
        prepare(
            tvChannel.currentProgramme?.title?.takeIf {
                it.trim().isNotBlank()
            }?.trim() ?: tvChannel.tvChannelName,
            tvChannel.currentProgramme?.description?.takeIf {
                it.isNotBlank()
            }?.trim() ?: tvChannel.tvGroup,
            false,
            showProgressManager = showProgressManager
        )
    }

    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
    }

    companion object {
        private const val EXTRA_EXTENSION_ID = "extra:extension_id"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        private const val EXTRA_TV_CHANNEL_LIST = "extra:tv_channel_list"
        fun newInstance(
            tvChannel: ExtensionsChannel,
            extension: ExtensionsConfig
        ) = FragmentExtensionsPlayback().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_EXTENSION_ID to extension

            )
        }
    }
}
package com.kt.apps.media.xemtv.ui.tv

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.HeaderItem
import com.kt.apps.core.base.leanback.ImageCardView
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.OnItemViewSelectedListener
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class FragmentTVDashboard : BaseRowSupportFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var filterGroup = FILTER_TOTAL
    private var filterType = PlaybackActivity.Type.TV

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }
    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }
    private var selectedView: ImageCardView? = null
    private var mBackgroundUri: String? = null
    private var mBackgroundTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterGroup = requireArguments().getString(EXTRA_FILTER_GROUP) ?: FILTER_TOTAL
        filterType = requireArguments().getParcelable(EXTRA_FILTER_TYPE) ?: PlaybackActivity.Type.TV
    }

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        adapter = mRowsAdapter
        if (tvChannelViewModel.tvChannelLiveData.value !is DataState.Success) {
            tvChannelViewModel.getListTVChannel(false)
        }
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is TVChannel) {
                mBackgroundUri = item.logoChannel
                startBackgroundTimer()
            }
        }

        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item !is TVChannel) {
                return@OnItemViewClickedListener
            }
            if (!item.isFreeContent) {
                showErrorDialog(content = "Đây là nội dung tính phí\r\nLiên hệ đội phát triển để có thêm thông tin")
                return@OnItemViewClickedListener
            }
            tvChannelViewModel.getListProgramForChannel(item)
            tvChannelViewModel.getLinkStreamForChannel(tvDetail = item)
            selectedView = itemViewHolder.view as ImageCardView
        }

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    mRowsAdapter.clear()
                    val channelWithCategory = it.data
                        .filter(filterTVByGroup(filterGroup, filterType))
                        .groupBy {
                            it.tvGroup
                        }
                    val dashboardTVChannelPresenter = DashboardTVChannelPresenter()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = try {
                            val gr = TVChannelGroup.valueOf(group)
                            HeaderItem(gr.value)
                        } catch (e: Exception) {
                            HeaderItem(group)
                        }
                        val adapter = ArrayObjectAdapter(dashboardTVChannelPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                    }
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }

                is DataState.Loading -> {
                }

                else -> {
                }
            }
        }
    }

    override fun onStop() {
        tvChannelViewModel.clearCurrentPlayingChannelState()
        super.onStop()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), 300)
    }

    private val mHandler = Handler(Looper.myLooper()!!)

    private fun updateBackground(uri: String?) {
        if (!isVisible) return
        if (isDetached) return
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(ContextCompat.getDrawable(requireActivity(), com.kt.apps.core.R.drawable.default_background))
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                    }
                })

        mBackgroundTimer?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(arguments)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState ?: return
        filterGroup = savedInstanceState.getString(EXTRA_FILTER_GROUP) ?: FILTER_TOTAL
        filterType = savedInstanceState.getParcelable(EXTRA_FILTER_TYPE) ?: PlaybackActivity.Type.TV
    }

    override fun onDestroyView() {
        progressManager.disableProgressBar()
        progressManager.setRootView(null)
        super.onDestroyView()
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    companion object {
        const val FILTER_TOTAL = "Tất cả"
        const val EXTRA_FILTER_GROUP = "extra:filter_group"
        const val EXTRA_FILTER_TYPE = "extra:filter_type"

        fun newInstance(filterGroup: String, type: PlaybackActivity.Type) = FragmentTVDashboard().apply {
            this.arguments = bundleOf(
                EXTRA_FILTER_GROUP to filterGroup,
                EXTRA_FILTER_TYPE to type
            )
        }

        fun filterTVByGroup(
            filterGroup: String,
            filterType: PlaybackActivity.Type
        ) = { channel: TVChannel ->
            if (filterGroup == FILTER_TOTAL) {
                filterTvOrRadio(channel, filterType)
            } else {
                channel.tvGroup == filterGroup && filterTvOrRadio(channel, filterType)
            }
        }

        private fun filterTvOrRadio(
            channel: TVChannel,
            filterType: PlaybackActivity.Type
        ) = if (filterType == PlaybackActivity.Type.TV) {
            !channel.isRadio
        } else {
            channel.isRadio
        }

        fun newInstance(filterGroup: String, type: PlaybackActivity.Type, mMainAdapter: MainFragmentAdapter) =
            newInstance(filterGroup, type).apply {
                this.mMainFragmentAdapter = mMainAdapter
            }

    }

}
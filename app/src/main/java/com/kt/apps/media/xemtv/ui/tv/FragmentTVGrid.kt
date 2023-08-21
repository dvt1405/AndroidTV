package com.kt.apps.media.xemtv.ui.tv

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseGridViewFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.base.leanback.BrowseSupportFragment.FragmentHostImpl
import com.kt.apps.core.base.leanback.FocusHighlight
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.OnItemViewSelectedListener
import com.kt.apps.core.base.leanback.RowsSupportFragment
import com.kt.apps.core.base.leanback.VerticalGridPresenter
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.FragmentTvGridBinding
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard.Companion.filterTVByGroup
import javax.inject.Inject

class FragmentTVGrid : BaseGridViewFragment<FragmentTvGridBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_tv_grid


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private var filterGroup = FragmentTVDashboard.FILTER_TOTAL
    private var filterType = PlaybackActivity.Type.TV

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            filterGroup =
                arguments?.getString(FragmentTVDashboard.EXTRA_FILTER_GROUP) ?: FragmentTVDashboard.FILTER_TOTAL
            filterType =
                arguments?.getParcelable(FragmentTVDashboard.EXTRA_FILTER_TYPE) ?: PlaybackActivity.Type.TV
        } else if (savedInstanceState != null) {
            filterGroup =
                savedInstanceState.getString(FragmentTVDashboard.EXTRA_FILTER_GROUP) ?: FragmentTVDashboard.FILTER_TOTAL
            filterType =
                savedInstanceState.getParcelable(FragmentTVDashboard.EXTRA_FILTER_TYPE) ?: PlaybackActivity.Type.TV
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FragmentTVDashboard.EXTRA_FILTER_GROUP, filterGroup)
        outState.putParcelable(FragmentTVDashboard.EXTRA_FILTER_TYPE, filterType)
    }

    override fun onCreatePresenter(): VerticalGridPresenter {
        return VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
            this.setLayoutRes(R.layout.fragment_tv_vertical_grid)
            this.shadowEnabled = false
            this.numberOfColumns = 5
        }
    }

    override fun onItemViewSelectedListener(): OnItemViewSelectedListener {
        return OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
        }
    }

    override fun onItemViewClickedListener(): OnItemViewClickedListener {
        return OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item !is TVChannel) {
                return@OnItemViewClickedListener
            }
            if (!item.isFreeContent && !this.isDetached && !this.isHidden) {
                showErrorDialog(content = "Đây là nội dung tính phí\r\nLiên hệ đội phát triển để có thêm thông tin")
                return@OnItemViewClickedListener
            }
            tvChannelViewModel.getLinkStreamForChannel(tvDetail = item)
        }
    }

    override fun onCreateAdapter() {
        val cardPresenter = TVChannelPresenterSelector(requireActivity())
        cardPresenter.defaultImageWidthDimensions = 150 * resources.displayMetrics.scaledDensity
        mAdapter = ArrayObjectAdapter(cardPresenter)
        updateAdapter()
    }

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    if (mAdapter == null) {
                        onCreateAdapter()
                    }
                    (mAdapter as ArrayObjectAdapter).clear()
                    binding.title.text = filterGroup
                    val channelWithCategory = it.data
                        .filter(filterTVByGroup(filterGroup, filterType))

                    (mAdapter as ArrayObjectAdapter)
                        .addAll(0, channelWithCategory)
                }

                else -> {

                }
            }
        }
    }

    companion object {
        fun newInstance(
            filterGroup: String,
            type: PlaybackActivity.Type,
            mMainAdapter: BrowseSupportFragment.MainFragmentAdapter<RowsSupportFragment>
        ) = FragmentTVGrid().apply {
            this.arguments = bundleOf(
                FragmentTVDashboard.EXTRA_FILTER_GROUP to filterGroup,
                FragmentTVDashboard.EXTRA_FILTER_TYPE to type
            )
            this.mMainFragmentAdapter.setFragmentHost(mMainAdapter.fragmentHost as FragmentHostImpl)
        }
    }
}
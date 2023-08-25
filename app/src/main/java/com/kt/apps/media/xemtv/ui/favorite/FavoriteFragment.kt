package com.kt.apps.media.xemtv.ui.favorite

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.BaseOnItemViewClickedListener
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.base.leanback.HeaderItem
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.base.leanback.Row
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FavoriteFragment : BaseRowSupportFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val favoriteViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[FavoriteViewModel::class.java]
    }
    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.base_favorite_fragment
    }
    private var mContainerListAlignTop: Int = 40.dpToPx()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context ?: return
        val ta = requireContext().obtainStyledAttributes(
            com.kt.apps.resources.R.style.Theme_BaseLeanBack_SearchScreen,
            androidx.leanback.R.styleable.LeanbackTheme
        )
        mContainerListAlignTop = ta.getDimension(
            androidx.leanback.R.styleable.LeanbackTheme_browseRowsMarginTop,
            requireContext().resources.getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_browse_rows_margin_top
            ).toFloat()
        ).toInt()
        ta.recycle()
    }
    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        if (mMainFragmentAdapter == null) {
            mMainFragmentAdapter = object : MainFragmentAdapter(this) {
                override fun setAlignment(windowAlignOffsetFromTop: Int) {
                    super.setAlignment(mContainerListAlignTop)
                }
            }
        }
        return mMainFragmentAdapter
    }

    override fun initView(rootView: View) {
        favoriteViewModel.getListFavorite()
        adapter = mRowsAdapter
        mainFragmentAdapter.fragmentHost.notifyDataReady(mMainFragmentAdapter)
    }

    override fun initAction(rootView: View) {
        onItemViewClickedListener = BaseOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row: Row ->
            if (item is VideoFavoriteDTO) {
                when (item.type) {
                    VideoFavoriteDTO.Type.TV, VideoFavoriteDTO.Type.RADIO -> tvChannelViewModel.getLinkStreamById(item.id)
                    VideoFavoriteDTO.Type.IPTV -> favoriteViewModel.getResultForItem(item)
                }
            }
        }

        tvChannelViewModel.tvWithLinkStreamLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Loading) {
                progressManager.show()
            } else {
                progressManager.hide()
            }
            if (it is DataState.Success) {
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.EXTRA_TV_CHANNEL, it.data)
                intent.putExtra(
                    PlaybackActivity.EXTRA_PLAYBACK_TYPE, if (it.data.channel.isRadio) {
                        PlaybackActivity.Type.RADIO
                    } else {
                        PlaybackActivity.Type.TV
                    } as Parcelable
                )
                startActivity(intent)
            }
        }

        var lastListData: List<VideoFavoriteDTO>? = null
        favoriteViewModel.listFavoriteLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    if (it.data.isEmpty()) {
                        mRowsAdapter.clear()
                        view?.findViewById<View>(R.id.ic_empty_search)?.visible()
                        return@observe
                    }
                    view?.findViewById<View>(R.id.ic_empty_search)?.gone()
                    if (it.data.reduceIdAndUrl() == lastListData?.reduceIdAndUrl()) {
                        return@observe
                    }
                    mRowsAdapter.clear()

                    val childPresenter = DashboardTVChannelPresenter()
                    for ((group, channelList) in it.data.groupBy {
                        it.type
                    }) {
                        val headerItem = try {
                            val gr = when (group) {
                                VideoFavoriteDTO.Type.TV -> "Truyền hình"
                                VideoFavoriteDTO.Type.RADIO -> "Phát thanh"
                                VideoFavoriteDTO.Type.IPTV -> "IPTV"
                            }
                            HeaderItem(gr)
                        } catch (e: Exception) {
                            HeaderItem("Truyền hình")
                        }

                        val adapter = ArrayObjectAdapter(childPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        val listRow = ListRow(headerItem, adapter)
                        mRowsAdapter.add(listRow)
                    }
                    lastListData = it.data
                }

                is DataState.Loading -> {
                    if (lastListData.isNullOrEmpty()) {
                        mRowsAdapter.applyLoading()
                    }
                }

                is DataState.Error -> {

                }

                else -> {

                }
            }
        }
        favoriteViewModel.selectedItem.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                val data = it.data
                Log.e("TAG", "${it.data}")
                startActivity(
                    Intent(
                        requireContext(),
                        PlaybackActivity::class.java
                    ).apply {
                        putExtra(
                            PlaybackActivity.EXTRA_PLAYBACK_TYPE,
                            PlaybackActivity.Type.EXTENSION as Parcelable
                        )
                        putExtra(
                            PlaybackActivity.EXTRA_ITEM_TO_PLAY,
                            data.channel
                        )
                        putExtra(
                            PlaybackActivity.EXTRA_EXTENSIONS_ID,
                            data.config
                        )
                    }
                )
            }
        }
    }

    private fun List<VideoFavoriteDTO>?.reduceIdAndUrl() = this?.map {
        "${it.id}${it.url}"
    }?.reduce { acc, s -> "$acc$s" }

    override fun getSelectedPosition(): Int {
        return super.getSelectedPosition()
    }

    override fun onResume() {
        super.onResume()
        favoriteViewModel.getListFavorite()
    }
}
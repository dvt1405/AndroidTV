package com.kt.apps.media.xemtv.ui.extensions

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import com.kt.apps.core.base.leanback.*
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FragmentExtensions : BaseRowSupportFragment() {

    @Inject
    lateinit var roomDataBase: RoomDataBase

    @Inject
    lateinit var parserExtensionsSource: ParserExtensionsSource

    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private val extensions : ExtensionsConfig by lazy {
        requireArguments().getParcelable(EXTRA_EXTENSIONS_ID)!!
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }

    private var tvList: List<ExtensionsChannel>? = null

    override fun initView(rootView: View) {
        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
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
                            item as ExtensionsChannel
                        )
                        putExtra(
                            PlaybackActivity.EXTRA_EXTENSIONS_ID,
                            extensions
                        )
                    }
                )
            }
    }

    override fun initAction(rootView: View) {
        adapter = mRowsAdapter
        Logger.e(this, message = "$extensions")
        mRowsAdapter.applyLoading(R.layout.item_tv_loading_presenter)
    }

    private var liveData: LiveData<DataState<List<ExtensionsChannel>>>? = null
    private var dataLoaded: Boolean = false
    override fun onStart() {
        super.onStart()
        liveData = extensionsViewModel.loadChannelForConfig(extensions.sourceUrl)
    }

    override fun onResume() {
        super.onResume()
        liveData?.observe(viewLifecycleOwner) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    if (dataLoaded && mRowsAdapter.size() > 0) {
                        return@observe
                    }
                    this@FragmentExtensions.tvList = dataState.data
                    val channelWithCategory = tvList!!.groupBy {
                        it.tvGroup
                    }.toSortedMap()
                    mRowsAdapter.clear()
                    val childPresenter = DashboardTVChannelPresenter()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = try {
                            val gr = TVChannelGroup.valueOf(group)
                            HeaderItem(gr.value)
                        } catch (e: Exception) {
                            HeaderItem(group)
                        }
                        val adapter = ArrayObjectAdapter(childPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                        dataLoaded = true
                    }
                }

                is DataState.Error -> {
                    dataLoaded = true
                    showErrorDialog(content = dataState.throwable.message)
                    Logger.e(this, exception = dataState.throwable)
                }

                is DataState.Loading -> {
                    dataLoaded = false
                }

                else -> {

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        liveData?.removeObservers(viewLifecycleOwner)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        private const val EXTRA_EXTENSIONS_ID = "extra:extensions_id"
        fun newInstance(extensionConfig: ExtensionsConfig) = FragmentExtensions().apply {
            this.arguments = bundleOf(
                EXTRA_EXTENSIONS_ID to extensionConfig
            )
        }
    }
}
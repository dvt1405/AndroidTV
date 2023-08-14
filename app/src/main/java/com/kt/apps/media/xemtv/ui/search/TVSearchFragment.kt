package com.kt.apps.media.xemtv.ui.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.R
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.IKeyCodeHandler
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.BrowseFrameLayout
import com.kt.apps.core.base.leanback.BrowseFrameLayout.OnChildFocusListener
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.base.leanback.HeaderItem
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.base.leanback.OnItemViewClickedListener
import com.kt.apps.core.base.leanback.SearchView
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.presenter.SearchPresenter
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class TVSearchFragment : BaseRowSupportFragment(), IKeyCodeHandler {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: SearchViewModels by lazy {
        ViewModelProvider(requireActivity(), factory)[SearchViewModels::class.java]
    }

    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private var _searchFilter: String? = null
    private var _queryHint: String? = null
    private var _searchView: SearchView? = null
    private var _rootView: BrowseFrameLayout? = null
    private var _btnClose: ImageView? = null
    private var _btnVoice: ImageView? = null
    private var _loadingIcon: ProgressBar? = null
    private var _emptySearchIcon: ImageView? = null
    private var autoCompleteView: SearchView.SearchAutoComplete? = null

    override fun getLayoutResourceId(): Int {
        return R.layout.base_lb_search_fragment
    }

    private var mContainerListAlignTop: Int = 35.dpToPx()
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
        _rootView = rootView as BrowseFrameLayout
        _searchView = rootView.findViewById(R.id.search_view)
        _btnVoice = rootView.findViewById(androidx.appcompat.R.id.search_voice_btn)
        _btnClose = rootView.findViewById(androidx.appcompat.R.id.search_close_btn)
        _emptySearchIcon = rootView.findViewById(R.id.ic_empty_search)
        _loadingIcon = rootView.findViewById(R.id.ic_loading)
        autoCompleteView = _searchView?.searchEdtAutoComplete
        _searchFilter = arguments?.getString(EXTRA_QUERY_FILTER)
        _queryHint = arguments?.getString(EXTRA_QUERY_HINT).takeIf {
            !it.isNullOrBlank()
        }
        viewModel.lastSearchQuery?.let {
            autoCompleteView?.setText(it)
        }

        adapter = mRowsAdapter
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is SearchForText.SearchResult -> {
                    viewModel.getResultForItem(
                        item,
                        _searchView?.searchEdtAutoComplete?.text.toString()
                    )
                }
            }
        }

        _searchView?.apply {
            val searchManager = (requireActivity().getSystemService(Context.SEARCH_SERVICE)) as SearchManager
            this.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            this.setIconifiedByDefault(true)
            this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.querySearch(newText, _searchFilter)
                    return true
                }

            })
            arguments?.getString(EXTRA_QUERY_KEY)?.let {
                _searchView?.showKeyBoardOnDefaultFocus = false
                if (it.isNotEmpty()) {
                    _btnClose?.requestFocus()
                }
                this.setQuery(it, true)
            }

            setQueryHint(this, _queryHint)
        }
    }


    private fun setQueryHint(searchView: SearchView?, queryHint: String?) {
        queryHint?.let {
            searchView?.queryHint = "Tìm kiếm trong $it"
        } ?: when (_searchFilter) {
            SearchForText.FILTER_ONLY_TV_CHANNEL -> {
                searchView?.queryHint = "Tìm kiếm kênh truyền hình"
            }

            SearchForText.FILTER_ALL_IPTV -> {
                searchView?.queryHint = "Tìm kiếm nội dung trong IPTV"
            }

            SearchForText.FILTER_FOOT_BALL -> {
                searchView?.queryHint = "Tìm kiếm trận đấu bóng đá"
            }

            else -> {
                searchView?.queryHint = "Tìm kiếm nội dung trên iMedia"
            }
        }
    }

    override fun initAction(rootView: View) {
        _searchView?.searchEdtAutoComplete?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val inputMethod = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethod.showSoftInput(v, 0)
            }
        }

        _searchView?.searchEdtAutoComplete?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val inputMethod = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethod.hideSoftInputFromWindow(v.windowToken, 0)
                mVerticalGridView.requestFocus()
            }
            return@setOnEditorActionListener true
        }

        _searchView?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        _rootView?.apply {
            this.setOnDispatchKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN
                    && selectedPosition == 0
                ) {
                    if (_searchView?.searchEdtAutoComplete?.text.isNullOrBlank()) {
                        _searchView?.searchEdtAutoComplete?.requestFocus()
                    } else if (_btnClose?.isFocused == false) {
                        _btnClose?.requestFocus()
                    } else {
                        return@setOnDispatchKeyListener false
                    }
                    return@setOnDispatchKeyListener true
                } else if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    val focused = view?.findFocus()
                    if (focused is DashboardTVChannelPresenter.TVImageCardView) {
                        _searchView?.searchEdtAutoComplete?.requestFocus()
                        return@setOnDispatchKeyListener true
                    } else if (focused == _searchView?.searchEdtAutoComplete) {
                        if (activity is TVSearchActivity) {
                            finishActivityIfNeeded()
                            return@setOnDispatchKeyListener true
                        }
                    }
                    return@setOnDispatchKeyListener false
                }
                return@setOnDispatchKeyListener false
            }

            this.onFocusSearchListener = BrowseFrameLayout.OnFocusSearchListener { focused, direction ->
                Logger.d(this@TVSearchFragment, message = "Focus search: $direction $focused")
                if (focused == _btnVoice && (direction == View.FOCUS_DOWN)
                    && 0 < verticalGridView.childCount
                ) {
                    return@OnFocusSearchListener verticalGridView
                } else if (focused == _btnVoice && (direction == View.FOCUS_RIGHT)) {
                    return@OnFocusSearchListener autoCompleteView
                } else if (focused == _btnClose) {
                    when (direction) {
                        View.FOCUS_DOWN, View.FOCUS_RIGHT -> {
                            if (0 < verticalGridView.childCount) {
                                return@OnFocusSearchListener verticalGridView
                            }
                        }

                        View.FOCUS_UP, View.FOCUS_LEFT -> {
                            return@OnFocusSearchListener autoCompleteView
                        }
                    }
                } else if (focused is AutoCompleteTextView && direction == View.FOCUS_DOWN) {
                    if (verticalGridView.childCount <= 0) {
                        _searchView?.showKeyboard()
                        return@OnFocusSearchListener focused
                    } else {
                        return@OnFocusSearchListener verticalGridView
                    }
                } else if (selectedPosition == 0 && direction == View.FOCUS_UP) {
                    return@OnFocusSearchListener _btnClose
                } else if (focused == verticalGridView && direction == View.FOCUS_UP) {
                    return@OnFocusSearchListener _btnClose
                } else if (direction == View.FOCUS_LEFT) {
                    if (verticalGridView.selectedSubPosition == 0){
                        return@OnFocusSearchListener null
                    }
                }
                return@OnFocusSearchListener focused
            }
            this.onChildFocusListener = object : OnChildFocusListener {
                override fun onRequestFocusInDescendants(direction: Int, previouslyFocusedRect: Rect?): Boolean {
                    if (direction == View.FOCUS_DOWN) {
                        if (verticalGridView.childCount == 0) {
                            if (_btnClose?.visibility == View.VISIBLE) {
                                _btnClose?.requestFocus()
                            } else {
                                findViewById<ImageView>(androidx.appcompat.R.id.search_voice_btn)
                                    ?.requestFocus()
                            }

                            return true
                        }
                    }
                    return false
                }

                override fun onRequestChildFocus(child: View?, focused: View?) {
                }
            }
        }
        viewModel.selectedItemLiveData.observe(viewLifecycleOwner, handleSelectedItem())
        viewModel.searchQueryLiveData.observe(this, handleSearchResult(autoCompleteView))
    }

    private fun handleSearchResult(autoCompleteView: SearchView.SearchAutoComplete?): (t: DataState<Map<String, List<SearchForText.SearchResult>>>) -> Unit =
        {
            if (it is DataState.Loading && !autoCompleteView?.text?.trim().isNullOrBlank()) {
                _loadingIcon?.visible()
                _loadingIcon?.isIndeterminate = true
                _emptySearchIcon?.gone()
            } else {
                _loadingIcon?.isIndeterminate = false
                _loadingIcon?.gone()
            }
            when (it) {
                is DataState.Success -> {
                    if (it.data.isEmpty()) {
                        _emptySearchIcon?.visible()
                    } else {
                        _emptySearchIcon?.gone()
                    }
                    verticalGridView?.visible()
                    val channelWithCategory = it.data
                    mRowsAdapter.clear()
                    val childPresenter = SearchPresenter()
                    childPresenter.filterString = autoCompleteView?.text?.toString()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = try {
                            val gr = TVChannelGroup.valueOf(group)
                            HeaderItem(gr.value)
                        } catch (e: Exception) {
                            HeaderItem(group)
                        }
                        headerItem.contentDescription = SearchForText.getHighlightTitle(
                            headerItem.name,
                            childPresenter.filterKeyWords
                        )
                        val adapter = ArrayObjectAdapter(childPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        val listRow = ListRow(headerItem, adapter)
                        mRowsAdapter.add(listRow)
                    }
                }

                else -> {

                }
            }
        }

    private fun handleSelectedItem(): (t: DataState<Any>) -> Unit = {
        if (it is DataState.Loading) {
            progressManager.show()
        } else {
            progressManager.hide()
        }
        when (it) {
            is DataState.Success -> {
                when (val data = it.data) {
                    is ExtensionsChannelAndConfig -> {
                        startActivity(
                            Intent(
                                requireActivity(),
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

                    is TVChannelLinkStream -> {
                        val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                        intent.putExtra(PlaybackActivity.EXTRA_TV_CHANNEL, data)
                        intent.putExtra(
                            PlaybackActivity.EXTRA_PLAYBACK_TYPE,
                            PlaybackActivity.Type.TV as Parcelable
                        )
                        startActivity(intent)
                    }
                }

            }

            else -> {

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        outState.putString(EXTRA_QUERY_FILTER, _searchFilter)
//        outState.putString(EXTRA_QUERY_HINT, _queryHint)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
//        if (savedInstanceState != null) {
//            savedInstanceState.getString(EXTRA_QUERY_FILTER)?.let {
//                _searchFilter = it
//            }
//            savedInstanceState.getString(EXTRA_QUERY_HINT).takeIf {
//                !it.isNullOrBlank()
//            }
//        }
    }

    override fun onDpadCenter() {
    }

    override fun onDpadDown() {
    }

    override fun onDpadUp() {
    }

    override fun onDpadLeft() {
    }

    override fun onDpadRight() {
    }

    override fun onKeyCodeChannelUp() {
    }

    override fun onKeyCodeChannelDown() {
        if (mBridgeAdapter.itemCount > 0 && view?.findViewById<SearchView>(R.id.search_view)?.isFocused == true) {
            verticalGridView.requestFocus()
        }
    }

    override fun onKeyCodeMediaPrevious() {
    }

    override fun onKeyCodeMediaNext() {
    }

    override fun onKeyCodeVolumeUp() {
    }

    override fun onKeyCodeVolumeDown() {
    }

    override fun onKeyCodePause() {
    }

    override fun onKeyCodePlay() {
    }

    override fun onKeyCodeMenu() {
    }

    private var lastRowSelected = 0

    override fun onRowSelected(
        parent: RecyclerView?,
        viewHolder: RecyclerView.ViewHolder?,
        position: Int,
        subposition: Int
    ) {
        super.onRowSelected(parent, viewHolder, position, subposition)
        lastRowSelected = position
    }

    fun onBackPressed() {
        val viewFocus = view?.findFocus()
        Logger.d(this, message = "onBackPressed view focused $viewFocus")
        _searchView?.searchEdtAutoComplete?.let {
            if (_searchView?.isFocused == true) {
                finishActivityIfNeeded()
            } else {
                it.requestFocus(View.FOCUS_UP)
            }
        } ?: finishActivityIfNeeded()
    }

    private fun finishActivityIfNeeded() {
        if (activity is TVSearchActivity) {
            if (CoreApp.activityCount == 1) {
                startActivity(Intent().apply {
                    this.data = Uri.parse("xemtv://tv/dashboard/")
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity?.finish()
            } else {
                activity?.finish()
            }
        } else {
            activity?.finish()
        }
    }

    fun onSearchFromDeeplink(query: String?, filter: String?, queryHint: String?) {
        arguments = arguments ?: bundleOf()
        arguments?.putString(EXTRA_QUERY_FILTER, filter)
        arguments?.putString(EXTRA_QUERY_HINT, queryHint)
        arguments?.putString(EXTRA_QUERY_KEY, query)
        _searchFilter = filter
        _queryHint = queryHint
        setQueryHint(_searchView, queryHint)
        _searchView?.setQuery(query, true)
        query?.takeIf {
            it.isNotEmpty() && it.isNotBlank()
        }?.let {
            _btnClose?.requestFocus()
            _searchView?.showKeyBoardOnDefaultFocus = false
        }
    }

    override fun onDestroyView() {
        _searchView = null
        _rootView = null
        _btnClose = null
        _btnVoice = null
        _emptySearchIcon = null
        autoCompleteView = null
        super.onDestroyView()
    }

    companion object {
        const val EXTRA_QUERY_KEY = "extra:query_key"
        const val EXTRA_QUERY_FILTER = "extra:query_filter"
        const val EXTRA_QUERY_HINT = "extra:query_hint"
    }

}
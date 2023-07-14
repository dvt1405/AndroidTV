package com.kt.apps.media.xemtv.ui.tv

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.ListRowPresenter
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.main.DashboardFragment
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

abstract class BaseTabLayoutFragment : BaseRowSupportFragment() {
    abstract val currentPage: Int
    abstract val tabLayout: LeanbackTabLayout
    abstract fun requestFocusChildContent(): View?

    class LoadingFragment : BaseRowSupportFragment() {
        override fun initView(rootView: View) {
        }

        override fun initAction(rootView: View) {
            adapter = ArrayObjectAdapter(ListRowPresenter().apply {
                shadowEnabled = false
            })
            (adapter as ArrayObjectAdapter).applyLoading(R.layout.item_tv_loading_presenter)
        }
    }

    abstract inner class BasePagerAdapter<T: Any>(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {
        protected val totalList by lazy {
            mutableListOf<T>()
        }

        abstract fun getFragment(position: Int): Fragment
        abstract fun getTabTitle(position: Int): CharSequence
        abstract fun getLoadingTabTitle(position: Int): CharSequence

        fun onRefresh(listItem: List<T>) {
            isLoading = false
            totalList.clear()
            totalList.addAll(listItem)
            notifyDataSetChanged()
        }

        var isLoading = false
        fun onLoading(listItem: List<T>) {
            if (totalList.isEmpty()) {
                isLoading = true
                totalList.addAll(listItem)
                notifyDataSetChanged()
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            if (`object`::class.java.name == LoadingFragment::class.java.name) {
                return POSITION_NONE
            }
            return super.getItemPosition(`object`)
        }

        override fun getCount(): Int {
            return totalList.size
        }

        override fun getItem(position: Int): Fragment {
            if (isLoading) {
                return LoadingFragment()
            }
            return getFragment(position)
        }

        override fun getPageTitle(position: Int): CharSequence {
            if (isLoading) {
                return getLoadingTabTitle(position)
            }
            return getTabTitle(position)
        }
    }
}

class FragmentTVDashboardNew : BaseTabLayoutFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    private val viewPager by lazy {
        requireView().findViewById<LeanbackViewPager>(R.id.view_pager)
    }

    private var type = PlaybackActivity.Type.TV

    override val currentPage: Int
        get() = requireView().findViewById<LeanbackViewPager>(R.id.view_pager).currentItem

    override val tabLayout: LeanbackTabLayout
        get() = requireView().findViewById(R.id.tab_layout)

    inner class TVViewPager(fragmentManager: FragmentManager) : BasePagerAdapter<String>(fragmentManager) {
        fun refreshPage(listTvChannel: List<TVChannel>) {
            isLoading = false
            totalList.clear()
            totalList.add(FragmentTVDashboard.FILTER_TOTAL)
            totalList.addAll(
                listTvChannel.groupBy {
                    it.tvGroup
                }.keys
            )
            notifyDataSetChanged()
        }

        fun onLoading() {
            onLoading(TVChannelGroup.values().map {
                it.value
            })
        }

        fun areContentTheSame(targetList: List<String>): Boolean {
            if (totalList.isEmpty()) {
                return false
            }
            return totalList.reduce { acc, s ->
                "$acc$s"
            } == targetList.reduce { acc, s ->
                "$acc$s"
            }
        }

        override fun getFragment(position: Int): Fragment {
            if (position != 0) {
                return FragmentTVGrid.newInstance(totalList[position], type, mMainFragmentAdapter)
            }
            return FragmentTVDashboard.newInstance(totalList[position], type, mMainFragmentAdapter)
        }

        override fun getTabTitle(position: Int): CharSequence {
            return if (position == 0) {
                totalList[position]
            } else {
                TVChannelGroup.valueOf(totalList[position]).value
            }
        }

        override fun getLoadingTabTitle(position: Int): CharSequence {
            return totalList[position]
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_tv_dashboard_new
    }

    private val pagerAdapter by lazy {
        TVViewPager(childFragmentManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = requireArguments().getParcelable(EXTRA_TYPE) ?: PlaybackActivity.Type.TV
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAlignment(mAlignedTop)
        MainActivity.screenWidth = view.findViewById<View>(R.id.view_pager).measuredWidth
    }

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        tvChannelViewModel.getListTVChannel(false)
        mainFragmentAdapter.fragmentHost!!.notifyViewCreated(mainFragmentAdapter)
        viewPager.adapter = pagerAdapter
        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    val totalList = it.data.filter(filterByType())
                    if (!pagerAdapter.areContentTheSame(totalList.groupBy {
                            it.tvGroup
                        }.keys.toList())
                    ) {
                        pagerAdapter.refreshPage(totalList)
                        tabLayout.setupWithViewPager(viewPager, true)
                    }
                    if (DashboardFragment.firstInit) {
                        tabLayout.getTabAt(0)?.view?.requestFocus()
                        DashboardFragment.firstInit = false
                    }
                }

                is DataState.Loading -> {
                    pagerAdapter.onLoading()
                    if (DashboardFragment.firstInit) {
                        tabLayout.getTabAt(0)?.view?.requestFocus()
                    }
                }


                else -> {

                }
            }
        }

        tvChannelViewModel.tvWithLinkStreamLiveData.observe(viewLifecycleOwner) {
            handleGetTVChannelLinkStream(it)
        }

    }

    private fun handleGetTVChannelLinkStream(it: DataState<TVChannelLinkStream>) {
        when (it) {
            is DataState.Loading -> {
            }

            is DataState.Success -> {
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

            is DataState.Error -> {
                showErrorDialog(content = it.throwable.message)
            }
            else -> {
            }
        }
        if (it is DataState.Loading) {
            progressManager.show()
        } else {
            progressManager.hide()
        }
    }

    private fun filterByType() = { channel: TVChannel ->
        if (type == PlaybackActivity.Type.TV) {
            !channel.isRadio
        } else {
            channel.isRadio
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(arguments)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.containsKey(EXTRA_TYPE) == true) {
            type = savedInstanceState.getParcelable(EXTRA_TYPE) ?: PlaybackActivity.Type.TV
        }
    }

    override fun requestFocusChildContent(): View? {
        return if (viewPager.adapter is TVViewPager) {
            val adapter = viewPager.adapter as TVViewPager
            if (adapter.isLoading) {
                return null
            } else {
                viewPager
            }
        } else {
            viewPager
        }
    }

    companion object {
        private const val EXTRA_TYPE = "extra:type"
        fun newInstance(dashboardType: PlaybackActivity.Type) = FragmentTVDashboardNew().apply {
            arguments = bundleOf(
                EXTRA_TYPE to dashboardType
            )
        }
    }

}
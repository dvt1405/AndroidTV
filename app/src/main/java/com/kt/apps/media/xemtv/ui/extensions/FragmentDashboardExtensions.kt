package com.kt.apps.media.xemtv.ui.extensions

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.GuidedStepSupportFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.leanback.findCurrentFocusedPosition
import com.kt.apps.core.utils.leanback.findCurrentFocusedView
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.search.TVSearchActivity
import com.kt.apps.media.xemtv.ui.tv.BaseTabLayoutFragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

class FragmentDashboardExtensions : BaseTabLayoutFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }


    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[ExtensionsViewModel::class.java]
    }

    private val viewPager by lazy {
        requireView().findViewById<LeanbackViewPager>(R.id.view_pager)
    }

    override val currentPage: Int
        get() = view?.findViewById<LeanbackViewPager>(R.id.view_pager)?.currentItem ?: 0

    override val tabLayout: LeanbackTabLayout?
        get() = view?.findViewById(R.id.tab_layout)

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_extensions_dashboard
    }

    private val pagerAdapter by lazy {
        ExtensionsChannelViewPager(childFragmentManager)
    }

    private var _btnAddSource: MaterialButton? = null
    private var _tabLayout: LeanbackTabLayout? = null
    private var _viewPager: LeanbackViewPager? = null

    override fun initView(rootView: View) {
        _btnAddSource = rootView.findViewById(R.id.btn_add_source)
        _viewPager = rootView.findViewById(R.id.view_pager)
        _tabLayout = rootView.findViewById(R.id.tab_layout)
    }

    override fun initAction(rootView: View) {
        extensionsViewModel.loadAllListExtensionsChannelConfig(false)
        setAlignment(mAlignedTop)
        _btnAddSource?.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    com.kt.skeleton.R.anim.fade_in, com.kt.skeleton.R.anim.fade_out,
                    com.kt.skeleton.R.anim.fade_in, com.kt.skeleton.R.anim.fade_out
                )
                .add(android.R.id.content, FragmentAddExtensions(), FragmentDashboardExtensions::class.java.name)
                .addToBackStack(FragmentDashboardExtensions::class.java.name)
                .commit()
        }

        extensionsViewModel.addExtensionConfigLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Update) {
                val updatedConfig = it.data
                val tabIndex = pagerAdapter.totalList.indexOfLast {
                    it.sourceUrl == updatedConfig.sourceUrl
                }

                if (tabIndex > -1) {
                    tabLayout?.getTabAt(tabIndex)?.text = updatedConfig.sourceName
                    pagerAdapter.onPageUpdate(tabIndex, updatedConfig)
                }
            }
        }

        extensionsViewModel.totalExtensionsConfig.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    val listConfig = it.data
                    if (listConfig.isNotEmpty()) {
                        if (!viewPager.isVisible) {
                            viewPager.currentItem = 0
                            val constrainSet = ConstraintSet()
                            constrainSet.clone(
                                LayoutInflater.from(requireContext())
                                    .inflate(R.layout.fragment_extensions_dashboard, null, false)
                                        as ConstraintLayout
                            )
                            constrainSet.applyTo(view as ConstraintLayout)
                        }
                    } else {
                        val constrainSet = ConstraintSet()
                        constrainSet.clone(
                            LayoutInflater.from(requireContext())
                                .inflate(R.layout.fragment_extensions_dashboard_empty_sence, null, false)
                                    as ConstraintLayout
                        )
                        constrainSet.applyTo(view as ConstraintLayout)
                    }
                    if (!pagerAdapter.areContentTheSame(listConfig)) {
                        pagerAdapter.onRefresh(listConfig)
                        viewPager.adapter = pagerAdapter
                        tabLayout?.setupWithViewPager(viewPager, true)
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

        val disposable = CompositeDisposable()

        tabLayout!!.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            for (i in 0 until tabLayout!!.tabCount) {
                tabLayout!!.getTabAt(i)?.view?.setOnLongClickListener {
                    val data = (extensionsViewModel.totalExtensionsConfig.value as? DataState.Success)?.data
                            ?: return@setOnLongClickListener false
                    val deleteSourceFragment = DeleteSourceFragment(
                        data[i],
                        progressManager,
                        disposable,
                        RoomDataBase.getInstance(requireContext()), {
                            extensionsViewModel.deleteExtensionConfig(data[i])
                            extensionsViewModel.loadAllListExtensionsChannelConfig(true)
                        }, {
                            extensionsViewModel.loadAllListExtensionsChannelConfig(true)
                        }
                    )
                    GuidedStepSupportFragment.addAsRoot(
                        requireActivity(),
                        deleteSourceFragment,
                        android.R.id.content
                    )
                    return@setOnLongClickListener true
                }
            }
        }

    }

    val btnAddSource: Button?
        get() = _btnAddSource

    override fun onDestroyView() {
        _btnAddSource = null
        _viewPager = null
        _tabLayout = null
        super.onDestroyView()
    }

    fun onFocusSearch(
        focused: View?,
        direction: Int
    ): View? {
        if (focused is TabLayout.TabView && direction == View.FOCUS_DOWN) {
            return if (extensionsViewModel.currentLiveDataConfig?.value is DataState.Loading
                || extensionsViewModel.currentLiveDataConfig?.value is DataState.Error
            ) {
                return focused
            } else {
                viewPager
            }
        }
        if (focused == _btnAddSource) {
            if (
                pagerAdapter.count == 0
                && (direction == View.FOCUS_RIGHT
                        || direction == View.FOCUS_DOWN
                        || direction == View.FOCUS_UP)
            ) {
                return _btnAddSource
            }
            if (direction == View.FOCUS_RIGHT) {
                if (tabLayout!!.tabCount > 0) {
                    return tabLayout?.getTabAt(0)?.view
                }
            } else if (direction == View.FOCUS_DOWN) {
                return if (extensionsViewModel.currentLiveDataConfig?.value is DataState.Loading
                    || extensionsViewModel.currentLiveDataConfig?.value is DataState.Error
                ) {
                    return focused
                } else {
                    viewPager
                }
            } else if (direction == View.FOCUS_UP) {
                startActivity(
                    Intent(
                        requireContext(),
                        TVSearchActivity::class.java
                    )
                )
                return null
            }
        }

        if (focused == tabLayout?.findCurrentFocusedView()) {
            val currentFocus = tabLayout!!.findCurrentFocusedPosition()
            val tabCount = tabLayout!!.tabCount
            if (direction == View.FOCUS_LEFT) {
                if (currentFocus == 0) {
                    return _btnAddSource
                }
            } else if (direction == View.FOCUS_RIGHT) {
                return if (currentFocus == tabCount - 1) {
                    focused
                } else {
                    tabLayout!!.getTabAt((currentFocus + 1) % tabLayout!!.tabCount)?.view
                }
            }
        }

        throw Throwable("Return to parent focus search")
    }

    override fun requestFocusChildContent(): View? {
        return _viewPager
    }

    class ExtensionsChannelViewPager(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val _totalList by lazy {
            mutableListOf<ExtensionsConfig>()
        }
        val totalList: List<ExtensionsConfig>
            get() = _totalList

        fun onPageUpdate(position: Int, extensionsConfig: ExtensionsConfig) {
            _totalList.removeAt(position)
            _totalList.add(position, extensionsConfig)
        }

        fun areContentTheSame(targetList: List<ExtensionsConfig>): Boolean {
            if (totalList.isEmpty() || targetList.isEmpty()) {
                return false
            }
            return totalList.map {
                it.sourceUrl
            }.reduce { acc, s ->
                "$acc$s"
            } == targetList.map {
                it.sourceUrl
            }.reduce { acc, s ->
                "$acc$s"
            }
        }

        fun onRefresh(extensionsConfigs: List<ExtensionsConfig>) {
            _totalList.clear()
            _totalList.addAll(extensionsConfigs)
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return _totalList.size
        }

        override fun getItem(position: Int): Fragment {
            return FragmentExtensions.newInstance(_totalList[position])
        }

        override fun getPageTitle(position: Int): CharSequence {
            return _totalList[position].sourceName
        }
    }

}
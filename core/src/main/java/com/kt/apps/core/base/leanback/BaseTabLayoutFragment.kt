package com.kt.apps.core.base.leanback

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.adapter.leanback.applyLoading

abstract class BaseTabLayoutFragment : BaseRowSupportFragment() {
    abstract val currentPage: Int
    abstract val tabLayout: LeanbackTabLayout?
    abstract fun requestFocusChildContent(): View?

    class LoadingFragment : BaseRowSupportFragment() {
        override fun initView(rootView: View) {
        }

        override fun initAction(rootView: View) {
            adapter = ArrayObjectAdapter(ListRowPresenter().apply {
                shadowEnabled = false
            })
            (adapter as ArrayObjectAdapter).applyLoading(com.kt.apps.resources.R.layout.item_tv_loading_presenter)
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
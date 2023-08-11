package com.kt.apps.media.mobile.ui.fragments.tv.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.IDashboardHelper

class TVDashboardAdapter(val fragment: Fragment, private val helper: IDashboardHelper) : FragmentStateAdapter(fragment){

    private val _listItemCategory by lazy {
        mutableListOf<String>()
    }

    override fun getItemCount(): Int {
        return _listItemCategory.size + 1
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0 ) {
            return helper.totalFragment()
        } else helper.perChannelFragment(_listItemCategory[position - 1])
    }

    fun getTitleForPage(position: Int): CharSequence {
        return if (position == 0) {
            fragment.getString(R.string.all)
        } else _listItemCategory[position - 1]
    }

    fun onRefresh(listItemCategory: Set<String>) {
        _listItemCategory.clear()
        _listItemCategory.addAll(listItemCategory)
        notifyDataSetChanged()
    }
}
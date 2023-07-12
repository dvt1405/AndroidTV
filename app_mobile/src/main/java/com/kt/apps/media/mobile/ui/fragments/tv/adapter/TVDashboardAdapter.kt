package com.kt.apps.media.mobile.ui.fragments.tv.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVChannelList

class TVDashboardAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val _listItemCategory by lazy {
        mutableListOf<String>()
    }

    override fun getItemCount(): Int {
        return _listItemCategory.size
    }

    override fun createFragment(position: Int): Fragment {
        return FragmentTVChannelList.newInstance(_listItemCategory[position])
    }

    fun getTitleForPage(position: Int): CharSequence {
        return TVChannelGroup.valueOf(
            _listItemCategory[position]
        ).value
    }

    fun onRefresh(listItemCategory: Set<String>) {
        _listItemCategory.clear()
        _listItemCategory.addAll(listItemCategory)
        notifyDataSetChanged()
    }
}
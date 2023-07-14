package com.kt.apps.media.mobile.ui.fragments.dashboard.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVDashboard
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment

class DashboardPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val _listItem by lazy {
        mutableListOf<Int>()
    }

    fun onRefresh(items: Sequence<Int>) {
        _listItem.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = _listItem.size

    override fun createFragment(position: Int): Fragment {
        return when (_listItem[position]) {
            R.id.tv -> FragmentTVDashboard(TVDashboardHelper())
            R.id.radio -> FragmentTVDashboard(RadioDashboardHelper())
            R.id.extension -> TVChannelsFragment() //FragmentIptvDashboard()
            R.id.search -> TVChannelsFragment() //FragmentSearch()
            R.id.football -> TVChannelsFragment()
//            R.id.info -> FragmentTVDashboard() //FragmentInfo()
            else -> throw IllegalStateException("Not support for item: ${_listItem[position]}")
        }
    }

    fun getPositionForItem(itemId: Int): Int {
        return _listItem.indexOf(itemId)
    }

    companion object {
        val pages by lazy {
            mapOf<Int, Int>()
        }
    }
}
package com.kt.apps.media.mobile.ui.fragments.dashboard.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.search.SearchDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.football.dashboard.FootballDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.iptv.IptvDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVDashboard

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
            R.id.extension -> IptvDashboardFragment() //FragmentIptvDashboard()
            R.id.search -> SearchDashboardFragment() //FragmentSearch()
            R.id.football -> FootballDashboardFragment()
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
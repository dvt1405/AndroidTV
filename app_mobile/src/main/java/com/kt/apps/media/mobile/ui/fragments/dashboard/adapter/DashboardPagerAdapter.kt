package com.kt.apps.media.mobile.ui.fragments.dashboard.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.ui.fragments.favorite.FavoriteFragment
import com.kt.apps.media.mobile.ui.fragments.search.SearchDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.football.dashboard.FootballDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.iptv.IptvDashboardFragment
import com.kt.apps.media.mobile.ui.fragments.tv.FragmentTVDashboard
import com.kt.apps.media.mobile.ui.info.InfoFragment

class DashboardPagerAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {
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
            R.id.tv -> FragmentTVDashboard.newInstance(false)
            R.id.radio -> FragmentTVDashboard.newInstance(true)
            R.id.extension -> IptvDashboardFragment.newInstance() //FragmentIptvDashboard()
            R.id.search -> SearchDashboardFragment.newInstance() //FragmentSearch()
            R.id.football -> FootballDashboardFragment.newInstance()
            R.id.info -> InfoFragment()
            R.id.favorite -> FavoriteFragment.newInstance()
            else -> throw IllegalStateException("Not support for item: ${_listItem[position]}")
        }
    }

    fun getPositionForItem(itemId: Int): Int {
        return _listItem.indexOf(itemId)
    }

    fun getItemOrNull(position: Int): Int? {
        return _listItem.getOrNull(position)
    }

    companion object {
        val pages by lazy {
            mapOf<Int, Int>()
        }
    }
}
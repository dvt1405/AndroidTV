package com.kt.apps.media.mobile.ui.fragments.football.dashboard

import android.os.Bundle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.football.list.FootballListFragment

class FootballDashboardFragment: BaseFragment<FragmentFootballDashboardBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_football_dashboard
    override val screenName: String
        get() = "FootballDashboard"

    private val footbalListFragment by lazy {
        FootballListFragment()
    }

    override fun initView(savedInstanceState: Bundle?) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, footbalListFragment)
            .commit()
    }

    override fun initAction(savedInstanceState: Bundle?) {

    }
}
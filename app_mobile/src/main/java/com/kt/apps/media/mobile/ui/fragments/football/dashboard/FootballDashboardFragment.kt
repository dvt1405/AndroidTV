package com.kt.apps.media.mobile.ui.fragments.football.dashboard

import android.os.Bundle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFootballDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.football.list.FootballListFragment
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FootballDashboardFragment: BaseFragment<FragmentFootballDashboardBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_football_dashboard
    override val screenName: String
        get() = "FootballDashboard"

    override fun initView(savedInstanceState: Bundle?) {

    }

    override fun initAction(savedInstanceState: Bundle?) {

    }

    companion object {
        fun newInstance(): FootballDashboardFragment {
            return FootballDashboardFragment()
        }
    }
}
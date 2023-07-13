package com.kt.apps.media.xemtv.ui.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import com.kt.apps.autoupdate.ui.FragmentInfo
import com.kt.apps.core.base.leanback.Row
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.R
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import com.kt.apps.core.logging.Logger
import com.kt.apps.media.xemtv.ui.extensions.FragmentDashboardExtensions
import com.kt.apps.media.xemtv.ui.football.FootballFragment
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import com.kt.apps.media.xemtv.ui.search.TVSearchFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboardNew

typealias OnFragmentChange = (pageID: Long) -> Unit
class DashboardPageRowFactory(
    private val backgroundManager: BackgroundManager,
) : BrowseSupportFragment.FragmentFactory<Fragment>() {
    var onFragmentChangeListener: OnFragmentChange? = null
    override fun createFragment(row: Any?): Fragment {
        val rowId = (row as? Row)?.id ?: throw IllegalStateException("Null row id")
        Logger.d(this, tag = "createFragment", message = "$rowId")
        backgroundManager.drawable = null
        onFragmentChangeListener?.invoke(rowId)
        return when (rowId) {
            ROW_TV -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                FragmentTVDashboardNew.newInstance(PlaybackActivity.Type.TV)
            }

            ROW_FOOTBALL -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.bg_football)
                FootballFragment()
            }

            ROW_RADIO -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                FragmentTVDashboardNew.newInstance(PlaybackActivity.Type.RADIO)
            }

            ROW_IPTV -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.bg_football)
                FragmentDashboardExtensions()
            }

            ROW_SEARCH -> {
                backgroundManager.drawable = ColorDrawable(Color.BLACK)
                TVSearchFragment()
            }

            ROW_INFO -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                FragmentInfo()
            }

            ROW_ADD_EXTENSION -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                FragmentDashboardExtensions()
            }

            else -> {
                try {
                    backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                    throw IllegalStateException("Not support row")
                } catch (e: Exception) {
                    throw IllegalStateException("Not support row")
                }
            }
        }
    }

    companion object {
        const val ROW_TV = 10999L
        const val ROW_FOOTBALL = 10998L
        const val ROW_RADIO = 10997L
        const val ROW_ADD_EXTENSION = 10996L
        const val ROW_IPTV = 10995L
        const val ROW_SEARCH = 10994L
        const val ROW_INFO = 10993L
    }
}
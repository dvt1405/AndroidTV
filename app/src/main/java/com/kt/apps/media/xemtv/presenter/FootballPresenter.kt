package com.kt.apps.media.xemtv.presenter

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.toColor
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.utils.getMainColor
import com.kt.apps.core.utils.loadImageBitmap
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.R

class FootballPresenter(private val showLeagueTitle: Boolean = true) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context)
            .inflate(R.layout.item_football_presenter, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (item as FootballMatch).apply {
            val home = viewHolder!!.view.findViewById<ImageView>(R.id.home_team)
            val away = viewHolder.view.findViewById<ImageView>(R.id.away_team)
            val matchTitle = viewHolder.view.findViewById<TextView>(R.id.match_name)
            val matchTime = viewHolder.view.findViewById<TextView>(R.id.match_time)
            val league = viewHolder.view.findViewById<TextView>(R.id.league)
            val cardView = viewHolder.view.findViewById<FrameLayout>(R.id.card_view)
            cardView.clipToOutline = true
            matchTitle.isSelected = true
            matchTitle.text = this.getMatchName()
            if (this.isLiveMatch) {
                matchTime.setText(com.kt.apps.core.R.string.football_live_match_title)
                matchTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    com.kt.apps.core.R.drawable.background_live_football_circle,
                    0, 0, 0
                )
            } else {
                matchTime.text = this.kickOffTime
                matchTime.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            if (showLeagueTitle) {
                league.text = this.league
            }
            val oldFocusChange = viewHolder.view.onFocusChangeListener
            viewHolder.view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                cardView.isSelected = hasFocus
                oldFocusChange.onFocusChange(v, hasFocus)
            }
            home.loadImageBitmap(this.homeTeam.logo,
                filterColor = if (this.homeTeam.name.lowercase().contains("juv")) {
                    Color.WHITE
                } else {
                    0
                },
                onResourceReady = {})
            away.loadImageBitmap(
                this.awayTeam.logo,
                filterColor = if (this.awayTeam.name.lowercase().contains("juv")) {
                    Color.WHITE
                } else {
                    0
                }
            ) {}
        }
    }

    private fun getColorForGradient(it: Bitmap): Long {
        val mainColorIntValue = it.getMainColor()
        val mainColor = mainColorIntValue.toColor()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.pack(
                mainColor.component1(),
                mainColor.component2(),
                mainColor.component3(),
                0.4f
            )
        } else {
            Color.argb(
                (0.4f * 256).toInt(),
                (mainColor.component1() * 256).toInt(),
                (mainColor.component2() * 256).toInt(),
                (mainColor.component3() * 256).toInt(),
            ).toLong()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }
}
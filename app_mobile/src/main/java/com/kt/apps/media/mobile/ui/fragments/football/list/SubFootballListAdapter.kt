package com.kt.apps.media.mobile.ui.fragments.football.list

import android.graphics.Color
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.loadImageBitmap
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballTeam
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemFootballChannelBinding
import com.kt.apps.resources.customview.ImageViewGradientBackground

open class SubFootballListAdapter: BaseAdapter<FootballMatch, ItemFootballChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_football_channel

    override fun bindItem(
        item: FootballMatch,
        binding: ItemFootballChannelBinding,
        position: Int,
        holder: BaseViewHolder<FootballMatch, ItemFootballChannelBinding>
    ) {
        with(binding) {
            homeTeam.loadFootballLogo(item.homeTeam)
            awayTeam.loadFootballLogo(item.awayTeam)
            matchName.text = item.getMatchName()
            matchName.isSelected = true
            cardView.clipToOutline = true
        }
    }

    private fun  ImageViewGradientBackground.loadFootballLogo(team: FootballTeam) {
        loadImageBitmap(team.logo, if (team.name.lowercase().contains("juv")) Color.WHITE else 0) { }
    }
}

class LiveSubFootballListAdapter: SubFootballListAdapter() {
    override fun bindItem(
        item: FootballMatch,
        binding: ItemFootballChannelBinding,
        position: Int,
        holder: BaseViewHolder<FootballMatch, ItemFootballChannelBinding>
    ) {
        super.bindItem(item, binding, position, holder)
        with(binding) {
            cardView.updateLayoutParams<ViewGroup.LayoutParams> {
                width = cardView.context.resources.getDimensionPixelSize(R.dimen.football_item_width_live)
                height = cardView.context.resources.getDimensionPixelSize(R.dimen.football_item_height_live)
            }
        }
    }
}
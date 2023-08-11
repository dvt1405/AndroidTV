package com.kt.apps.media.mobile.ui.fragments.football.list

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FootballItemRowChannelBinding
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.groupAndSort

data class FootballAdapterType(
    val data: Pair<String, List<FootballMatch>>,
    val isLive: Boolean
)

class FootballListAdapter: BaseAdapter<FootballAdapterType, FootballItemRowChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.football_item_row_channel
    var onChildItemClickListener: (FootballMatch, Int) -> Unit = { _, _ -> }
    override fun bindItem(
        item: FootballAdapterType,
        binding: FootballItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<FootballAdapterType, FootballItemRowChannelBinding>
    ) {
        val itemData = item.data
        binding.title.text = itemData.first
        with(binding.tvChannelChildRecyclerView) {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false).apply  {
                initialPrefetchItemCount = itemData.second.size
                isItemPrefetchEnabled = true
            }
            setHasFixedSize(true)
            clearOnScrollListeners()

            addItemDecoration(channelItemDecoration)
            adapter = if(item.isLive) {
                LiveSubFootballListAdapter()
            } else {
                SubFootballListAdapter()
            }.apply {
                onRefresh(itemData.second)
                onItemRecyclerViewCLickListener = { item, position ->
                    this@FootballListAdapter.onChildItemClickListener(item, position)
                }
            }
        }
        if (item.isLive) {
            binding.title.textSize = 28f
            binding.onLiveIcon.visibility = View.VISIBLE
        } else {
            binding.title.textSize = 20f
            binding.onLiveIcon.visibility = View.GONE
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder<FootballAdapterType, FootballItemRowChannelBinding>) {
        super.onViewRecycled(holder)
        with(holder.viewBinding.tvChannelChildRecyclerView) {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }
    }
}
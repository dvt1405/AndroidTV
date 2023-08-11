package com.kt.apps.media.mobile.ui.fragments.tv.adapter

import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentTvHorizontalChannelListBinding

class TVChannelsHorizontalAdapter(
    private val parentViewPool: RecycledViewPool
) : BaseAdapter<Map<String, List<TVChannel>>, FragmentTvHorizontalChannelListBinding>() {

    override val itemLayoutRes: Int
        get() = R.layout.fragment_tv_horizontal_channel_list

    override fun bindItem(
        item: Map<String, List<TVChannel>>,
        binding: FragmentTvHorizontalChannelListBinding,
        position: Int,
        holder: BaseViewHolder<Map<String, List<TVChannel>>, FragmentTvHorizontalChannelListBinding>
    ) {
        binding.horizontalRecyclerView.setRecycledViewPool(parentViewPool)
        val adapter = TVChannelListAdapter()
        binding.horizontalRecyclerView.adapter = adapter
        adapter.onRefresh(_listItem[0]["VTV"]!!)
    }
}
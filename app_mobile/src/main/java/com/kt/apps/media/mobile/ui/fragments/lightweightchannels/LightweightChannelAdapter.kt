package com.kt.apps.media.mobile.ui.fragments.lightweightchannels

import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemRowChannelBinding
import com.kt.apps.media.mobile.databinding.LightItemChannelBinding
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment.Companion.TAG
import com.kt.apps.media.mobile.ui.main.IChannelElement

class LightweightChannelAdapter: BaseAdapter<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

    var onChildItemClickListener: OnItemRecyclerViewCLickListener<IChannelElement>? = null
    override fun bindItem(
        item: Pair<String, List<IChannelElement>>,
        binding: ItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>
    ) {
        Log.d(TAG, "bindItem LightweightChannelAdapter: $item.first")
        binding.title.text =  item.first
        val spanCount = if (binding.root.context.resources.getBoolean(R.bool.is_landscape)) 4 else 2
        binding.tvChannelChildRecyclerView.apply {
            layoutManager = (object : GridLayoutManager(
                binding.root.context,
                spanCount,
                VERTICAL,
                false) {
                override fun canScrollHorizontally(): Boolean {
                    return false
                }

                override fun canScrollVertically(): Boolean {
                    return false
                }
            }).apply {
                initialPrefetchItemCount = item.second.size
                isItemPrefetchEnabled = true
            }

            setHasFixedSize(true)
            clearOnScrollListeners()
            adapter = SubChannelAdapter().apply {
                onRefresh(item.second)
                onItemRecyclerViewCLickListener = {childItem, childPosition ->
                    this@LightweightChannelAdapter.onChildItemClickListener?.invoke(childItem, position + childPosition)
                }
            }
        }
    }
}

private class SubChannelAdapter: BaseAdapter<IChannelElement, LightItemChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.light_item_channel

    override fun bindItem(
        item: IChannelElement,
        binding: LightItemChannelBinding,
        position: Int,
        holder: BaseViewHolder<IChannelElement, LightItemChannelBinding>
    ) {
        binding.item = item
    }
}
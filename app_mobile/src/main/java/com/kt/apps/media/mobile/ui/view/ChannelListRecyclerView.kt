package com.kt.apps.media.mobile.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FootballItemRowChannelBinding
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.databinding.ItemRowChannelBinding
import com.kt.apps.media.mobile.ui.fragments.football.list.FootballAdapterType
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.channelItemDecoration

data class ChannelListData(val title: String, val items: List<IChannelElement>)

class ChannelListRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val adapter by lazy { ChannelListAdapter() }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.main_channel_recycler_view)
    }
    init {
        View.inflate(context, R.layout.channel_list_recyclerview, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView.apply {
            adapter = this@ChannelListRecyclerView.adapter
            layoutManager = LinearLayoutManager(context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
        }
    }

    fun reloadAllData(list: List<ChannelListData>) {
        adapter.onRefresh(list)
    }
}

class ChannelListAdapter: BaseAdapter<ChannelListData, ItemRowChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

    override fun bindItem(
        item: ChannelListData,
        binding: ItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<ChannelListData, ItemRowChannelBinding>
    ) {
        binding.title.text = item.title
        with(binding.tvChannelChildRecyclerView) {
            layoutManager = if (itemCount > 1) {
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false).apply {
                    initialPrefetchItemCount = item.items.size
                    isItemPrefetchEnabled = true
                }
            } else {
                FlexboxLayoutManager(binding.root.context).apply {
                    isItemPrefetchEnabled = true
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.FLEX_START
                }
            }
            addItemDecoration(channelItemDecoration)
            setHasFixedSize(true)
            clearOnScrollListeners()
            adapter = ChildChannelAdapter().apply {
                onRefresh(item.items)
                this.onItemRecyclerViewCLickListener = { item, childPosition ->
//                    onChildItemClickListener?.invoke(item, position + childPosition)
                }
            }
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder<ChannelListData, ItemRowChannelBinding>) {
        super.onViewRecycled(holder)
        with(holder.viewBinding.tvChannelChildRecyclerView) {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }
    }
    class ChildChannelAdapter : BaseAdapter<IChannelElement, ItemChannelBinding>() {
        override val itemLayoutRes: Int
            get() = R.layout.item_channel

        override fun bindItem(
            item: IChannelElement,
            binding: ItemChannelBinding,
            position: Int,
            holder: BaseViewHolder<IChannelElement, ItemChannelBinding>
        ) {
            binding.item = item
            binding.title.isSelected = true
            binding.logo.loadImgByDrawableIdResName(item.logoChannel, item.logoChannel)
        }

        override fun onViewRecycled(holder: BaseViewHolder<IChannelElement, ItemChannelBinding>) {
            super.onViewRecycled(holder)
            holder.viewBinding.logo.setImageBitmap(null)
        }
    }

}
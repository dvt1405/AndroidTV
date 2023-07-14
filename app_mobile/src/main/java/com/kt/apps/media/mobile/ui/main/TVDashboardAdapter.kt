package com.kt.apps.media.mobile.ui.main

import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.databinding.ItemRowChannelBinding
import com.kt.apps.media.mobile.utils.LinearLayoutPagerManager

interface IChannelElement {
    val name: String
    val logoChannel: String
}

sealed class ChannelElement {
    class TVChannelElement(val model: TVChannel): IChannelElement {
        override val name: String
            get() = model.tvChannelName

        override val logoChannel: String
            get() = model.logoChannel
    }

    class ExtensionChannelElement(val model: ExtensionsChannel): IChannelElement {
        override val name: String
            get() = model.tvChannelName

        override val logoChannel: String
            get() = model.logoChannel
    }
}


class TVDashboardAdapter : BaseAdapter<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>() {
    var spanCount = 3
    var onChildItemClickListener: OnItemRecyclerViewCLickListener<IChannelElement>? = null
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<Pair<String, List<IChannelElement>>, ItemRowChannelBinding> {
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun onRefresh(
        items: List<Pair<String, List<IChannelElement>>>,
        notifyDataSetChange: Boolean
    ) {
        super.onRefresh(items, notifyDataSetChange)
        Log.d(TAG, "onRefresh: $items")
    }

    override fun bindItem(
        item: Pair<String, List<IChannelElement>>,
        binding: ItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>
    ) {
//        Logger.d(this, message = "${binding.tvChannelChildRecyclerView.width}")
        Log.d(TAG, "bindItem: ${item.first} $position")
        binding.title.text = item.first

        val layoutManager = LinearLayoutPagerManager(
            binding.root.context,
            LinearLayoutManager.HORIZONTAL,
            false,
            spanCount
        )

        layoutManager.initialPrefetchItemCount = item.second.size
        layoutManager.isItemPrefetchEnabled = true
        binding.tvChannelChildRecyclerView.layoutManager = layoutManager
        binding.tvChannelChildRecyclerView.setHasFixedSize(true)
        binding.tvChannelChildRecyclerView.clearOnScrollListeners()
        binding.tvChannelChildRecyclerView.adapter = ChildChannelAdapter().apply {
            onRefresh(item.second)
            this.onItemRecyclerViewCLickListener = { item, childPosition ->
                onChildItemClickListener?.invoke(item, position + childPosition)
            }
        }
    }


    override fun onViewRecycled(holder: BaseViewHolder<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>) {
        super.onViewRecycled(holder)
        holder.cacheItem = holder.viewBinding.tvChannelChildRecyclerView.adapter
        Logger.d(this, message = "OnView recycler")
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


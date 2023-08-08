package com.kt.apps.media.mobile.ui.main

import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.databinding.ItemRowChannelBinding
import com.kt.apps.media.mobile.utils.LinearLayoutPagerManager
import com.kt.apps.media.mobile.utils.channelItemDecoration

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

    class SearchExtension(val model: SearchForText.SearchResult.ExtensionsChannelWithCategory): IChannelElement {
        override val name: String
            get() = model.data.tvChannelName
        override val logoChannel: String
            get() = model.data.logoChannel
    }

    class SearchHistory(val model: SearchForText.SearchResult.History): IChannelElement {
        override val name: String
            get() = model.data.displayName
        override val logoChannel: String
            get() = model.data.thumb
    }

    class SearchTV(val model: SearchForText.SearchResult.TV): IChannelElement {
        override val name: String
            get() = model.data.tvChannelName
        override val logoChannel: String
            get() = model.data.logoChannel

    }
}


class TVDashboardAdapter : BaseAdapter<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>() {
    var onChildItemClickListener: OnItemRecyclerViewCLickListener<IChannelElement>? = null
    var preferWidth = Int.MAX_VALUE
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

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

        with(binding.tvChannelChildRecyclerView) {
            layoutManager = if (itemCount > 1) {
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false).apply {
                    initialPrefetchItemCount = item.second.size
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
            adapter = ChildChannelAdapter(preferWidth).apply {
                onRefresh(item.second)
                this.onItemRecyclerViewCLickListener = { item, childPosition ->
                    onChildItemClickListener?.invoke(item, position + childPosition)
                }
            }
        }
    }


    override fun onViewRecycled(holder: BaseViewHolder<Pair<String, List<IChannelElement>>, ItemRowChannelBinding>) {
        super.onViewRecycled(holder)
        holder.cacheItem = holder.viewBinding.tvChannelChildRecyclerView.adapter
        with(holder.viewBinding.tvChannelChildRecyclerView) {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }
        Logger.d(this, message = "OnView recycler")
    }


    class ChildChannelAdapter(val preferWidth: Int) : BaseAdapter<IChannelElement, ItemChannelBinding>() {
        override val itemLayoutRes: Int
            get() = R.layout.item_channel

        override fun bindItem(
            item: IChannelElement,
            binding: ItemChannelBinding,
            position: Int,
            holder: BaseViewHolder<IChannelElement, ItemChannelBinding>
        ) {
            with(binding) {
                val currentWidth = binding.logo.context.resources
                    .getDimensionPixelSize(R.dimen.item_channel_width)
                val currentHeight = binding.logo.context.resources
                    .getDimensionPixelSize(R.dimen.item_channel_height)

                if (currentWidth > preferWidth) {
                    val newWidth = preferWidth
                    val newHeight = preferWidth * currentHeight / currentWidth
                    logo.updateLayoutParams<ViewGroup.LayoutParams> {
                        width = newWidth
                        height = newHeight
                    }
                }
            }
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


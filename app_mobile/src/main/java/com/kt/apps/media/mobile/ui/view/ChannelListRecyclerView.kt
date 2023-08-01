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
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ChannelListData(val title: String, val items: List<IChannelElement>)

class ChannelListRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    enum class Mode {
        FLEX, LINEAR
    }
    private var mode: Mode = Mode.LINEAR
    private val adapter by lazy { ChannelListAdapter().apply {
            onChildItemClickListener = { item, position ->
                this@ChannelListRecyclerView.onChildItemClickListener(item, position)
            }
        }
    }
    private val linearLayoutManager by lazy {
        LinearLayoutManager(context)

    }
    private val singleAdapter by lazy { ChannelListAdapter.ChildChannelAdapter().apply {
        onItemRecyclerViewCLickListener = { item, position ->
            this@ChannelListRecyclerView.onChildItemClickListener(item, position)
        }
    } }

    private val singleLayoutManager by lazy {
        FlexboxLayoutManager(this.context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.main_channel_recycler_view)
    }
    private val skeletonScreen by lazy {
        recyclerView.let {
            KunSkeleton.bind(it)
                .itemCount(10)
                .recyclerViewLayoutItem(
                    R.layout.item_row_channel_skeleton,
                    R.layout.item_channel_skeleton
                )
                .build()
        }
    }
    var onChildItemClickListener: (IChannelElement, Int) -> Unit = { _,_ -> }
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
            isNestedScrollingEnabled = true
        }
    }

    fun reloadAllData(list: List<ChannelListData>) {
//        adapter.onRefresh(list)
        if (list.size == 1) {
            if (
                recyclerView.layoutManager !is FlexboxLayoutManager
                || recyclerView.adapter != singleAdapter
                    ) {
                recyclerView.adapter = singleAdapter
                recyclerView.layoutManager = singleLayoutManager
                recyclerView.addItemDecoration(channelItemDecoration)
            }
            singleAdapter.onRefresh(list[0].items)
            mode = Mode.FLEX
        } else {
            if (
                recyclerView.layoutManager !is LinearLayoutManager
                || recyclerView.adapter != adapter
            ) {
                recyclerView.adapter = adapter
                recyclerView.layoutManager = linearLayoutManager
                recyclerView.removeItemDecoration(channelItemDecoration)
            }
            adapter.onRefresh(list)
            mode = Mode.LINEAR
        }
    }

    fun showHideSkeleton(isShow: Boolean) {
        if (isShow) {
            skeletonScreen.run {  }
        } else {
            skeletonScreen.hide {
                recyclerView.adapter = when(mode) {
                    Mode.FLEX -> singleAdapter
                    Mode.LINEAR -> adapter
                }
                recyclerView.layoutManager = when(mode) {
                    Mode.FLEX -> singleLayoutManager
                    Mode.LINEAR -> linearLayoutManager
                }
            }
        }
    }
}

class ChannelListAdapter: BaseAdapter<ChannelListData, ItemRowChannelBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

    var onChildItemClickListener: (IChannelElement, Int) -> Unit = { _,_ -> }
    override fun bindItem(
        item: ChannelListData,
        binding: ItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<ChannelListData, ItemRowChannelBinding>
    ) {
        binding.title.text = item.title
        binding.tvChannelChildRecyclerView.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false).apply {
                initialPrefetchItemCount = item.items.size
                isItemPrefetchEnabled = true
            }
            addItemDecoration(channelItemDecoration)
            setHasFixedSize(true)
            clearOnScrollListeners()
            adapter = ChildChannelAdapter().apply {
                onRefresh(item.items)
                this.onItemRecyclerViewCLickListener = { item, childPosition ->
//                    onChildItemClickListener?.invoke(item, position + childPosition)
                    this@ChannelListAdapter.onChildItemClickListener(item, position + childPosition)
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

data class ChildItem(val data: IChannelElement, val position: Int)
fun ChannelListRecyclerView.childItemClicks(): Flow<ChildItem> {
    return callbackFlow {
        this@childItemClicks.onChildItemClickListener = { item, position ->
            trySend(ChildItem(item, position))
        }
        awaitClose {
            this@childItemClicks.onChildItemClickListener = { _,_ -> }
        }
    }
}
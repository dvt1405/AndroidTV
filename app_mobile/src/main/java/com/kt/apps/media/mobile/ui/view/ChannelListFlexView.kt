package com.kt.apps.media.mobile.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.channelItemDecoration
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.*

class ChannelListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    enum class DisplayStyle {
        FLEX, HORIZONTAL_LINEAR
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.main_channel_recycler_view)
    }
    var onChildItemClickListener: (IChannelElement, Int) -> Unit = { _, _ -> }

    private val _adapter by lazy { Adapter().apply {
        onItemRecyclerViewCLickListener = { item, position ->
            this@ChannelListView.onChildItemClickListener(item, position)
        }
    } }

    init {
        View.inflate(context, R.layout.channel_list_recyclerview, this)
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        recyclerView?.setOnTouchListener(l)
    }

    fun addOnScrollListener(l: RecyclerView.OnScrollListener) {
        recyclerView?.addOnScrollListener(l)
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView.apply {
            adapter = this@ChannelListView._adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setItemViewCacheSize(9)
            isNestedScrollingEnabled = false
            addItemDecoration(channelItemDecoration)
            setHasFixedSize(false)
        }
    }
    fun changeDisplayStyle(style: DisplayStyle) {
        recyclerView.layoutManager = when(style) {
            DisplayStyle.FLEX -> FlexboxLayoutManager(context).apply {
                isItemPrefetchEnabled = true
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
            DisplayStyle.HORIZONTAL_LINEAR -> LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = true
            }
        }
    }

    fun reloadAllData(list: List<IChannelElement>) {
        _adapter.onRefresh(list)
        MainScope().launch {
            delay(200)
            recyclerView.stopScroll()
        }
    }


    class Adapter: BaseAdapter<IChannelElement, ItemChannelBinding>() {
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
            Log.d(TAG, "onViewRecycled: ${holder.viewBinding.title.text}")
        }
    }
}


package com.kt.apps.media.mobile.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.GridAutoFitLayoutManager
import com.kt.apps.media.mobile.utils.channelItemDecoration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChannelListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    enum class DisplayStyle {
        GRID, HORIZONTAL_LINEAR
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.main_channel_recycler_view)
    }
    var onChildItemClickListener: (IChannelElement, Int) -> Unit = { _, _ -> }

    private val _adapter by lazy { RowItemChannelAdapter().apply {
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

    fun clearAdapter() {
        recyclerView.adapter = null
        _adapter.onRefresh(emptyList())
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView.apply {
            adapter = this@ChannelListView._adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = false
            }
            isNestedScrollingEnabled = false
            addItemDecoration(channelItemDecoration)
            setHasFixedSize(false)
        }
    }
    fun changeDisplayStyle(style: DisplayStyle) {
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
            isItemPrefetchEnabled = false
        }
        recyclerView.layoutManager = when(style) {
            DisplayStyle.GRID -> GridAutoFitLayoutManager(context, resources.getDimension(R.dimen.item_channel_width).toInt() + channelItemDecoration.padding)
            DisplayStyle.HORIZONTAL_LINEAR -> LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                isItemPrefetchEnabled = false
            }
        }
    }

    fun reloadAllData(list: List<IChannelElement>) {
        _adapter.onRefresh(list)
    }

    fun forceRedraw() {
        recyclerView.adapter = null
        recyclerView.adapter = _adapter
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) =
        recyclerView.setPadding(left, top, right, bottom)

}

class RowItemChannelAdapter: BaseAdapter<IChannelElement, ItemChannelBinding>() {

    override val itemLayoutRes: Int
        get() = R.layout.item_channel

    override fun bindItem(
        item: IChannelElement,
        binding: ItemChannelBinding,
        position: Int,
        holder: BaseViewHolder<IChannelElement, ItemChannelBinding>
    ) {
        Log.d(TAG, "bindItem: $position ${item.name} ${item.logoChannel}")
        binding.item = item
        binding.title.isSelected = true
        if (item.isUseDrawable) {
            binding.logo.loadImgByDrawableIdResName(item.logoChannel, item.logoChannel)
        } else {
            binding.logo.loadImgByUrl(item.logoChannel)
        }

    }

    override fun onViewRecycled(holder: BaseViewHolder<IChannelElement, ItemChannelBinding>) {
        super.onViewRecycled(holder)
        Log.d(TAG, "onViewRecycled: ${holder.viewBinding.title.text}")
        holder.viewBinding.logo.setImageBitmap(null)
    }
}

fun ChannelListView.childClicks() : Flow<IChannelElement> {
    return callbackFlow {
        onChildItemClickListener = { item, position ->
            trySend(item)
        }

        awaitClose {
            onChildItemClickListener = { _, _ ->}
        }
    }
}

fun RowItemChannelAdapter.childClicks(): Flow<IChannelElement> {
    return callbackFlow {
        onItemRecyclerViewCLickListener = { item, pos ->
            trySend(item)
        }

        awaitClose { onItemRecyclerViewCLickListener = { _, _ -> } }
    }
}

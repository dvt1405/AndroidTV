package com.kt.apps.media.mobile.ui.main

import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding

class TVChannelAdapter : BaseAdapter<TVChannel, ItemChannelBinding>() {

    override val itemLayoutRes: Int
        get() = R.layout.item_channel

    override fun bindItem(
        item: TVChannel,
        binding: ItemChannelBinding,
        position: Int,
        holder: BaseViewHolder<TVChannel, ItemChannelBinding>
    ) {
//        binding.item = item
        binding.title.isSelected = true
        binding.logo.loadImgByDrawableIdResName(item.logoChannel, item.logoChannel)
    }

    override fun onViewRecycled(holder: BaseViewHolder<TVChannel, ItemChannelBinding>) {
        super.onViewRecycled(holder)
        holder.viewBinding.logo.setImageBitmap(null)
    }


}
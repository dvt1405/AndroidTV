package com.kt.apps.media.mobile.ui.fragments.channels

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.AdapterView.OnItemLongClickListener
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemSectionBinding
import com.kt.skeleton.convertDpToPixel

interface SectionItem {
    val displayTitle: String
    val id: Int
    val icon: Drawable
}


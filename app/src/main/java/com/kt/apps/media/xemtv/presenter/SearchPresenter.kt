package com.kt.apps.media.xemtv.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kt.apps.core.Constants
import com.kt.apps.core.base.leanback.ImageCardView
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.getKeyForLocalLogo
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import kotlin.properties.Delegates

class SearchPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()
    private var _filterHighlight: List<String>? = null

    var filterString: String?
        set(value) {
            _filterHighlight = value?.lowercase()?.trim()
                ?.replaceVNCharsToLatinChars()
                ?.split(" ")
                ?.filter {
                    it.isNotBlank() && it.isNotEmpty()
                }?.flatMap {
                    val unSpecialChar = it.removeAllSpecialChars()
                    if (it != unSpecialChar) {
                        return@flatMap listOf(it, unSpecialChar)
                    }
                    return@flatMap listOf(it)
                }
        }
        get() = _filterHighlight?.reduce { acc, s ->
            "$acc $s"
        }

    val filterKeyWords: List<String>?
        get() = _filterHighlight

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(
            parent.context,
            com.kt.apps.resources.R.drawable.app_icon
        )
        val cardView: ImageCardView = DashboardTVChannelPresenter.TVImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView

        when (item) {
            is SearchForText.SearchResult.ExtensionsChannelWithCategory -> {
                cardView.titleText = item.highlightTitle
                cardView.contentText = ""
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
                updateCardBackgroundColor(cardView, false)
                cardView.let { imgView ->
                    val name = Constants.mapChannel[
                            item.data
                                .tvChannelName
                                .getKeyForLocalLogo()
                    ]
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    name?.let {
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(it, item.data.logoChannel.trim())
                    } ?: imgView.mainImageView.loadImgByUrl(item.data.logoChannel.trim())
                }
            }

            is SearchForText.SearchResult.History -> {
                cardView.titleText = item.data.displayName
                cardView.contentText = item.data.category
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
                updateCardBackgroundColor(cardView, false)
                item.data.type
                cardView.let { imgView ->
                    val name = Constants.mapChannel[
                        item.data
                            .displayName
                            .getKeyForLocalLogo()
                    ]
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    name?.let {
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(it, item.data.thumb.trim())
                    } ?: imgView.mainImageView.loadImgByUrl(item.data.thumb.trim())
                }
            }

            is SearchForText.SearchResult.TV -> {
                cardView.titleText = item.highlightTitle
                cardView.contentText = null
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

                cardView.let { imgView ->
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imgView.mainImageView
                        .loadImgByDrawableIdResName(item.data.logoChannel, item.data.logoChannel)
                }
                updateCardBackgroundColor(cardView, false)
            }

        }

    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        view.findViewById<TextView>(androidx.leanback.R.id.title_text)
            .background = null
        view.background = null
        view.infoAreaBackground = null
    }

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}
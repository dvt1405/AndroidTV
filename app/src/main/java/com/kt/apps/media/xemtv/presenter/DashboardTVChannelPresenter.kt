package com.kt.apps.media.xemtv.presenter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.kt.apps.core.base.leanback.ImageCardView
import com.kt.apps.core.base.leanback.Presenter
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.kt.apps.core.Constants
import com.kt.apps.core.utils.dpToPx

import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.storage.local.dto.VideoFavoriteDTO
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.getKeyForLocalLogo
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.media.xemtv.R
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class DashboardTVChannelPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()
    private var _defaultImageWidthDimensions: Float = CARD_WIDTH.toFloat()
    var defaultImageWidthDimensions: Float?
        get() = _defaultImageWidthDimensions
        set(value) {
            if (value != null) {
                _defaultImageWidthDimensions = value
            }
        }

    class TVImageCardView(context: Context) :
        ImageCardView(ContextThemeWrapper(context, R.style.ImageCardViewStyleTitle)) {
        override fun setSelected(selected: Boolean) {
            with(findViewById<TextView>(androidx.leanback.R.id.title_text)) {
                this.background = null
                this.isSelected = selected
            }
            background = null
            infoAreaBackground = null
            super.setSelected(selected)
        }

        override fun invalidate() {
            super.invalidate()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, com.kt.apps.resources.R.drawable.app_icon)

        val cardView: ImageCardView = TVImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        cardView.setMainImageDimensions(
            _defaultImageWidthDimensions.toInt(),
            (_defaultImageWidthDimensions / IMAGE_DIMENSION).toInt()
        )
        when (item) {
            is VideoFavoriteDTO -> {
                cardView.titleText = item.title
                cardView.contentText = null

                if (item.type == VideoFavoriteDTO.Type.IPTV) {
                    cardView.let { imgView ->
                        val name = Constants.mapChannel[
                                (item.id.takeIf {
                                    it.trim().isNotBlank()
                                } ?: item.logoUrl).getKeyForLocalLogo()
                        ]
                        imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        name?.let {
                            imgView.mainImageView
                                .loadImgByDrawableIdResName(it, item.logoUrl.trim())
                        } ?: imgView.mainImageView.loadImgByUrl(item.logoUrl.trim())
                    }
                } else {
                    cardView.let { imgView ->
                        imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(item.logoUrl, item.logoUrl)
                    }
                }
                updateCardBackgroundColor(cardView, false)
            }

            is TVChannel -> {
                cardView.titleText = item.tvChannelName
                cardView.contentText = null

                cardView.let { imgView ->
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imgView.mainImageView
                        .loadImgByDrawableIdResName(item.logoChannel, item.logoChannel)
                }
                updateCardBackgroundColor(cardView, false)
            }

            is ExtensionsChannel -> {
                cardView.titleText = item.tvChannelName
                cardView.contentText = ""
                updateCardBackgroundColor(cardView, false)
                cardView.let { imgView ->
                    val name = Constants.mapChannel[
                            (item.channelId.takeIf {
                                it.trim().isNotBlank()
                            } ?: item.tvChannelName).getKeyForLocalLogo()
                    ]
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    name?.let {
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(it, item.logoChannel.trim())
                    } ?: imgView.mainImageView.loadImgByUrl(item.logoChannel.trim())
                }
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
        val CARD_WIDTH = 160.dpToPx() //dp
        private const val CARD_HEIGHT = 176
        private const val IMAGE_DIMENSION = 48.0 / 27
    }
}
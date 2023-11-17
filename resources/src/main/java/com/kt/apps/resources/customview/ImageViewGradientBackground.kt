package com.kt.apps.resources.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import com.kt.apps.resources.R

class ImageViewGradientBackground(
    _context: Context,
    _attrs: AttributeSet,
) : AppCompatImageView(_context, _attrs, 0) {
    private val _bound: Bound
    private val _imageWidth: Int
    private val _imageHeight: Int

    init {
        if (corner == null) {
            corner = context.resources.displayMetrics.scaledDensity * CORNER_IN_DP
        }
        val a = context.obtainStyledAttributes(_attrs, R.styleable.ImageViewGradientBackground)
        _bound = when (a.getInt(R.styleable.ImageViewGradientBackground_rounded, Bound.NONE.value)) {
            Bound.LEFT.value -> Bound.LEFT
            Bound.RIGHT.value -> Bound.RIGHT
            Bound.ALL.value -> Bound.ALL
            else -> Bound.NONE
        }
        _imageWidth = a.getDimensionPixelSize(
            R.styleable.ImageViewGradientBackground_imageWidth,
            R.dimen.def_image_width
        )
        _imageHeight = a.getDimensionPixelSize(
            R.styleable.ImageViewGradientBackground_imageWidth,
            R.dimen.def_image_height
        )
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val paddingHorizontal = (measuredWidth - _imageWidth) / 2
        val paddingVertical = (measuredHeight - _imageHeight) / 2
        this.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        bm ?: return
        val mainColor = bm.getMainColor()
        this.background = getBackgroundGradient(mainColor)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        this.background = getBackgroundGradient(
            ContextCompat.getColor(context, R.color.color_text_highlight)
                .toColor()
        )
    }

    private val gradientOrientation = when (_bound) {
        Bound.LEFT -> GradientDrawable.Orientation.LEFT_RIGHT
        Bound.RIGHT -> GradientDrawable.Orientation.RIGHT_LEFT
        else -> GradientDrawable.Orientation.TOP_BOTTOM
    }

    private val gradientCornerBound = when (_bound) {
        Bound.LEFT -> leftBound
        Bound.RIGHT -> rightBound
        Bound.ALL -> allBound
        else -> floatArrayOf()
    }

    private fun getBackgroundGradient(color: Color): Drawable {
        val gradientColor = intArrayOf(
            changeWithAlpha(color, 0.4f),
            Color.TRANSPARENT,
            Color.TRANSPARENT,
        )
        return GradientDrawable(
            gradientOrientation,
            gradientColor
        ).apply {
            this.cornerRadii = gradientCornerBound
        }
    }

    private fun Bitmap.getMainColor(): Color = Palette.from(this)
        .generate()
        .getDarkVibrantColor(ContextCompat.getColor(context, R.color.black))
        .toColor()

    private fun changeWithAlpha(color: Color, alpha: Float): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.pack(
                color.component1(),
                color.component2(),
                color.component3(),
                alpha
            ).toColorInt()
        } else {
            Color.argb(
                (alpha * 256).toInt(),
                (color.component1() * 256).toInt(),
                (color.component2() * 256).toInt(),
                (color.component3() * 256).toInt(),
            )
        }
    }

    class APiUnder26Reflection(colorObject: Color) {
        private val rgbValue: FloatArray

        init {
            val components = colorObject::class.java.getDeclaredField("mComponents")
            components.isAccessible = true
            rgbValue = components.get(colorObject) as FloatArray
        }

        fun red(): Float {
            return rgbValue[0]
        }

        fun blue(): Float {
            return rgbValue[1]
        }

        fun green(): Float {
            return rgbValue[2]
        }
    }

    private enum class Bound(val value: Int) {
        RIGHT(0),
        LEFT(1),
        ALL(2),
        NONE(3)
    }

    companion object {
        private const val CORNER_IN_DP = 9
        private var corner: Float? = null
        private val rightBound by lazy {
            floatArrayOf(
                0f, 0f,
                corner!!, corner!!,
                corner!!, corner!!,
                0f, 0f
            )
        }

        private val leftBound by lazy {
            floatArrayOf(
                corner!!, corner!!,
                0f, 0f,
                0f, 0f,
                corner!!, corner!!,
            )
        }

        private val allBound by lazy {
            floatArrayOf(
                corner!!, corner!!,
                corner!!, corner!!,
                corner!!, corner!!,
                corner!!, corner!!,
            )
        }
    }


}
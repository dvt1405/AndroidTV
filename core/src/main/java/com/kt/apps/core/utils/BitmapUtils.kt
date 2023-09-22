package com.kt.apps.core.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.*
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kt.apps.core.GlideApp
import com.kt.apps.core.GlideRequest
import com.kt.apps.resources.R
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.blurry.Blur
import com.kt.apps.core.utils.blurry.BlurFactor
import java.util.concurrent.Executors

private val BITMAP_THREAD_POOL = Executors.newCachedThreadPool()

fun Bitmap.getMainColor(): Int = Palette.from(this)
    .generate()
    .let {
        val vibrant = it.getVibrantColor(
            ContextCompat.getColor(CoreApp.getInstance(), R.color.black)
        )

        val dark = it.getDarkVibrantColor(
            ContextCompat.getColor(CoreApp.getInstance(), R.color.black)
        )
        dark
    }

fun ImageView.loadImageBitmap(
    url: String,
    @ColorInt filterColor: Int = 0,
    onResourceReady: (bitmap: Bitmap) -> Unit,
) {
    GlideApp.with(this)
        .asBitmap()
        .load(url)
        .centerInside()
        .addListener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadImageBitmap.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.app_icon)
                )
                return true
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                resource?.let {
                    onResourceReady(it)
                }
                this@loadImageBitmap.setImageBitmap(resource)
                this@loadImageBitmap.setColorFilter(filterColor)
                return true
            }


        })
        .into(this)
}

fun ImageView.loadImgByUrl(url: String, scaleType: ScaleType = ScaleType.CENTER_INSIDE) {
    GlideApp.with(this)
        .asBitmap()
        .load(url.trim())
        .error(R.drawable.app_banner)
        .override(170, 120)
        .scaleType(scaleType)
        .fitCenter()
        .addListener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadImgByUrl.setImageBitmap(resource)
                BITMAP_THREAD_POOL.execute {
                    val bitmap = Blur.of(context, resource, BlurFactor().apply {
                        this.width =
                            ((resource?.width ?: 50) / 2 * context.resources.displayMetrics.scaledDensity).toInt()
                        this.height =
                            ((resource?.height ?: 120) / 2 * context.resources.displayMetrics.scaledDensity).toInt()
                        this.radius = 10
                        this.sampling = 1
                    })

                    Handler(Looper.getMainLooper()).post {
                        this@loadImgByUrl.background = BitmapDrawable(context.resources, bitmap)
                    }
                }
                return true
            }


        })
        .into(this)
}

fun ImageView.loadDrawableRes(@DrawableRes @RawRes resId: Int, scaleType: ScaleType = ScaleType.CENTER_INSIDE) {
    GlideApp.with(this)
        .load(resId)
        .error(R.drawable.app_banner)
        .scaleType(scaleType)
        .into(this)
}

fun ImageView.loadImgByDrawableIdResName(
    name: String,
    backupUrl: String? = null,
    scaleType: ScaleType = ScaleType.CENTER_INSIDE
) {

    Log.d(TAG, "loadImgByDrawableIdResName: $name")
    val context = context.applicationContext
    val id = context.resources.getIdentifier(
        name.trim().removeSuffix(".png")
            .removeSuffix(".jpg")
            .removeSuffix(".webp")
            .removeSuffix(".jpeg"),
        "drawable",
        context.packageName
    )
    try {
        val drawable = ContextCompat.getDrawable(context, id)
        GlideApp.with(this)
            .load(drawable)
            .override(410, 230)
            .scaleType(scaleType)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.app_banner)
            .into(this)
    } catch (e: Exception) {
        Logger.e(this, name, e)
        backupUrl?.let { url ->
            loadImgByUrl(url.trim(), scaleType)
        } ?: loadDrawableRes(R.drawable.app_banner, scaleType)
    }

}

fun <TranscodeType> GlideRequest<TranscodeType>.scaleType(scaleType: ScaleType): GlideRequest<TranscodeType> {
    return when (scaleType) {
        ScaleType.FIT_XY -> {
            this.optionalFitCenter()
        }
        ScaleType.CENTER_CROP -> {
            this.optionalCenterCrop()
        }
        else -> {
            this.optionalCenterInside()
        }
    }
}
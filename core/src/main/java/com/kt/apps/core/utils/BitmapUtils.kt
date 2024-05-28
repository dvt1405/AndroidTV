package com.kt.apps.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.Matrix4f
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicColorMatrix
import android.renderscript.ScriptIntrinsicResize
import android.util.Log
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.kt.apps.core.GlideApp
import com.kt.apps.core.GlideRequest
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.utils.blurry.Blur
import com.kt.apps.core.utils.blurry.BlurFactor
import com.kt.apps.resources.R
import java.util.concurrent.Executors

val red50PercentMatrix = floatArrayOf(
    0.7f, 0f, 0f, 0f,
    0f, 0.7f, 0f, 0f,
    0f, 0f, 0.7f, 0f,
    0f, 0f, 0f, 1f
)

private val BITMAP_THREAD_POOL = Executors.newCachedThreadPool()

private val MAIN_HANDLER = Handler(Looper.getMainLooper())

object ImageBlurUtils {

    fun blur(context: Context, originalBitmap: Bitmap, backgroundColor: Int, blurRadius: Float): Bitmap {
        val rs = RenderScript.create(context)
        val bitmap = Bitmap.createBitmap(
            originalBitmap.width / 4,
            originalBitmap.height / 4,
            Bitmap.Config.ARGB_8888
        )
        //Resize the image to 1/16th of the original size
        val outAllocation = Allocation.createFromBitmap(rs, bitmap)
        ScriptIntrinsicResize.create(rs).apply {
            setInput(Allocation.createFromBitmap(rs, originalBitmap))
            forEach_bicubic(outAllocation)
        }
        outAllocation.copyTo(bitmap)

        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript.setInput(Allocation.createFromBitmap(rs, bitmap))
        blurScript.setRadius(blurRadius)
        blurScript.forEach(outAllocation)
        outAllocation.copyTo(bitmap)

        val colorMatrixScript = ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs))
        colorMatrixScript.setColorMatrix(Matrix4f(red50PercentMatrix))
        colorMatrixScript.forEach(Allocation.createFromBitmap(rs, bitmap), outAllocation)
        outAllocation.copyTo(bitmap)

        rs.destroy()
        return bitmap
    }

    private fun applyBackgroundColor(bitmap: Bitmap, color: Int) {
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val newColor = averageColors(color, pixel)
                bitmap.setPixel(x, y, newColor)
            }
        }
    }
}

fun averageColors(color1: Int, color2: Int): Int {
    val alpha = (color2 shr 24 and 0xff + color1 shr 24 and 0xff) / 2
    val red = (color2 shr 16 and 0xff + color1 shr 16 and 0xff) / 2
    val green = (color2 shr 8 and 0xff + color1 shr 8 and 0xff) / 2
    val blue = (color2 and 0xff + color1 and 0xff shr 1) / 2
    return alpha shl 24 or (red shl 16) or (green shl 8) or blue
}

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

            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@loadImgByUrl.setImageBitmap(resource)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this@loadImgByUrl.setImageBitmap(resource)
                    try {
                        BITMAP_THREAD_POOL.execute {
                            val imgReader = ImageReader.newInstance(
                                resource?.width ?: 50,
                                resource?.height ?: 120,
                                1,
                                1,
                                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                            )
                            val renderNode = RenderNode("BlurEffect")
                            val hardwareRenderer = HardwareRenderer()
                            hardwareRenderer.setSurface(imgReader.surface)
                            hardwareRenderer.setContentRoot(renderNode)
                            renderNode.setPosition(0, 0, imgReader.width * 4, imgReader.height * 4)
                            val blurEffect = RenderEffect.createBlurEffect(
                                50f, 50f,
                                Shader.TileMode.MIRROR
                            )
                            renderNode.setRenderEffect(blurEffect)
                            val renderCanvas = renderNode.beginRecording()
                            val backgroundBitmap = resource!!.copy(Bitmap.Config.ARGB_8888, true)
                            renderCanvas.drawBitmap(
                                backgroundBitmap,
                                0f,
                                0f,
                                null
                            )
                            renderNode.endRecording()
                            hardwareRenderer.createRenderRequest()
                                .setWaitForPresent(true)
                                .syncAndDraw()
                            val image =
                                imgReader.acquireNextImage() ?: throw RuntimeException("No Image")
                            val hardwareBuffer =
                                image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                                ?: throw RuntimeException("Create Bitmap Failed")
                            MAIN_HANDLER.post {
                                this@loadImgByUrl.background = BitmapDrawable(context.resources, bitmap)
                            }
                            hardwareBuffer.close()
                            image.close()
                            imgReader.close()
                            renderNode.discardDisplayList()
                            hardwareRenderer.destroy()
                        }

                    } catch (e: Exception) {
                        blurBackgroundBelowAndroidS(resource)
                    }
                } else {
                    blurBackgroundBelowAndroidS(resource)
                }
                return true
            }

            private fun blurBackgroundBelowAndroidS(resource: Bitmap?) {
                BITMAP_THREAD_POOL.execute {
                    val newBitmap = try {
                        ImageBlurUtils.blur(
                            context,
                            resource!!,
                            Color.WHITE,
                            25f
                        )
                    } catch (e: Exception) {
                        Blur.of(context, resource, BlurFactor().apply {
                            this.width =
                                ((resource?.width
                                    ?: 50) / 2 * context.resources.displayMetrics.scaledDensity).toInt()
                            this.height =
                                ((resource?.height
                                    ?: 120) / 2 * context.resources.displayMetrics.scaledDensity).toInt()
                            this.radius = 10
                            this.sampling = 1
                        })
                    }
                    MAIN_HANDLER.post {
                        this@loadImgByUrl.background = BitmapDrawable(context.resources, newBitmap)
                    }
                }
            }


        })
        .into(this)
}

private fun getDominantColor(bitmap: Bitmap): Int {
    val palette = Palette.from(bitmap).generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch

    return swatch?.rgb ?: 0
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
    try {
        val context = context.applicationContext
        val id = context.resources.getIdentifier(
            name.trim().removeSuffix(".png")
                .removeSuffix(".jpg")
                .removeSuffix(".webp")
                .removeSuffix(".jpeg"),
            "drawable",
            context.packageName
        )
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
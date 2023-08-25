package com.kt.apps.autoupdate.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.kt.apps.autoupdate.R
import com.kt.apps.autoupdate.databinding.FragmentQrCodeBinding
import com.kt.apps.core.base.BaseFragment

class FragmentQrCode : BaseFragment<FragmentQrCodeBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_qr_code
    override val screenName: String
        get() = "FragmentQrCode"

    override fun initView(savedInstanceState: Bundle?) {

    }

    override fun initAction(savedInstanceState: Bundle?) {
        val strToGenerateQrCode = if (savedInstanceState == null) {
            arguments?.getString(EXTRA_STR_TO_GENERATE_QR_CODE)
        } else {
            savedInstanceState.getString(EXTRA_STR_TO_GENERATE_QR_CODE)
        }
        binding.qrCode.setImageBitmap(
            encodeAsBitmap(
                strToGenerateQrCode,
                (250 * resources.displayMetrics.scaledDensity).toInt()
            )
        )
    }

    private fun encodeAsBitmap(str: String?, widthPx: Int): Bitmap? {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            str,
            BarcodeFormat.QR_CODE,
            widthPx,
            widthPx,
            mapOf(
                EncodeHintType.MARGIN to 1
            )
        )
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    companion object {
        private const val EXTRA_STR_TO_GENERATE_QR_CODE = "extra:generate_qr_code"
        fun newInstance(strToGenerateQrCode: String) = FragmentQrCode().apply {
            arguments = bundleOf(
                EXTRA_STR_TO_GENERATE_QR_CODE to strToGenerateQrCode
            )
        }
    }
}
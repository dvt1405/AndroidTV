package com.kt.apps.media.mobile.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentInfoBinding

class InfoFragment : BaseFragment<FragmentInfoBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_info
    override val screenName: String
        get() = "InfoFragment"

    override fun initView(savedInstanceState: Bundle?) {
        binding.appName.text = "iMedia version ${BuildConfig.VERSION_CODE}"
        binding.faceBookTv.text = "http://fb.com/imediaapp"
        binding.zaloTv.text = "http://zalo.me/imediaapp"

        binding.faceBookTv.setOnClickListener {
            openURL(binding.faceBookTv.text.toString())
        }

        binding.zaloTv.setOnClickListener {
            openURL(binding.zaloTv.text.toString())
        }

        binding.updateCheck.setOnClickListener {
            openURL("https://play.google.com/store/apps/details?id=com.kt.apps.media.mobile.xemtv")
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {

    }

    fun openURL(url: String) {
        var url = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            InfoFragment()
    }


}
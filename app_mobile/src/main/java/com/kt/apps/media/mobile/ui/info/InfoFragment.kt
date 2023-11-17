package com.kt.apps.media.mobile.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentInfoBinding
import com.kt.apps.voiceselector.VoiceSelectorManager
import org.json.JSONObject
import javax.inject.Inject

class InfoFragment : BaseFragment<FragmentInfoBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_info
    override val screenName: String
        get() = "InfoFragment"

    private var fbLink = "https://fb.com/groups/imediaapp/"
    private var zlLink = "https://zalo.me/g/bcdftf650"
    private val playstoreLink = "https://play.google.com/store/apps/details?id=com.kt.apps.media.mobile.xemtv"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var voiceSelectorManager: VoiceSelectorManager

    override fun initView(savedInstanceState: Bundle?) {
        binding.appName.text = getString(com.kt.apps.autoupdate.R.string.version_title, BuildConfig.VERSION_NAME.removePrefix("Mobile."))
        binding.facebookBtn.setOnClickListener {
            openURL(fbLink)
        }
        binding.zaloBtn.setOnClickListener {
            openURL(zlLink)
        }
        binding.imediaLink.setOnClickListener {
            openURL(binding.imediaLink.contentDescription.toString())
        }
        binding.updateCheck.setOnClickListener {
            openURL(playstoreLink)
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        getFbZlGroupsLink()
    }

    private fun openURL(url: String) {
        var url = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    private fun getFbZlGroupsLink(retryTimes: Int = 3) {
        Firebase.remoteConfig
            .fetch()
            .addOnSuccessListener {
                try {
                    val jsonGroups = JSONObject(Firebase.remoteConfig.getString("user_groups"))
                    jsonGroups.optString("facebook").takeIf {
                        it.isNotEmpty() && it.isNotBlank()
                    }?.let {
                        fbLink = it
                    }
                    jsonGroups.optString("zalo").takeIf {
                        it.isNotEmpty() && it.isNotBlank()
                    }?.let {
                        zlLink = it
                    }
                    jsonGroups.optString("imedia").takeIf {
                        it.isNotEmpty() && it.isNotBlank()
                    }?.let {
                        binding.imediaLink.contentDescription = it
                    }
                } catch (_: Exception) {
                }
            }
            .addOnFailureListener {
                if (retryTimes > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        getFbZlGroupsLink(retryTimes - 1)
                    }, 2000)
                }
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            InfoFragment()
    }


}
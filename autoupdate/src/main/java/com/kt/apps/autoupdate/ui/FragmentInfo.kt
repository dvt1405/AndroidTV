package com.kt.apps.autoupdate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.autoupdate.R
import com.kt.apps.autoupdate.databinding.FragmentInfoBinding
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.BrowseFrameLayout
import com.kt.apps.core.base.leanback.BrowseSupportFragment
import org.json.JSONObject
import javax.inject.Inject

class FragmentInfo : BaseFragment<FragmentInfoBinding>(), BrowseSupportFragment.MainFragmentAdapterProvider {
    override val layoutResId: Int
        get() = R.layout.fragment_info
    override val screenName: String
        get() = "FragmentInfo"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[AppUpdateViewModel::class.java]
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.versionTitle.text = getString(R.string.version_title, appVersion)

    }

    override fun initAction(savedInstanceState: Bundle?) {
        binding.btnCheckUpdate.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.kt.apps.media.xemtv")
                )
            )
        }

        binding.fbGroupLink.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, FragmentQrCode.newInstance(binding.fbGroupLink.text.trim().toString()))
                .addToBackStack(null)
                .commit()
        }

        binding.zaloGroupLink.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, FragmentQrCode.newInstance(binding.zaloGroupLink.text.trim().toString()))
                .addToBackStack(null)
                .commit()
        }

        (binding.root as BrowseFrameLayout).setOnFocusSearchListener { focused, direction ->
            if (direction == View.FOCUS_LEFT) {
                return@setOnFocusSearchListener null
            }
            if (direction == View.FOCUS_DOWN) {
                when (focused) {
                    binding.btnCheckUpdate -> return@setOnFocusSearchListener binding.fbGroupLink
                    binding.fbGroupLink -> return@setOnFocusSearchListener binding.zaloGroupLink
                }
            } else if (direction == View.FOCUS_UP) {
                when (focused) {
                    binding.fbGroupLink -> return@setOnFocusSearchListener binding.btnCheckUpdate
                    binding.zaloGroupLink -> return@setOnFocusSearchListener binding.fbGroupLink
                }
            }
            return@setOnFocusSearchListener focused
        }

        viewModel.checkUpdateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    val data = it.data
                }

                else -> {

                }
            }
        }
        getFbZlGroupsLink()
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
                        binding.fbGroupLink.text = it
                    }
                    jsonGroups.optString("zalo").takeIf {
                        it.isNotEmpty() && it.isNotBlank()
                    }?.let {
                        binding.zaloGroupLink.text = it
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

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return BrowseSupportFragment.MainFragmentAdapter(this)
    }

    companion object {
        var appVersion: String = "23.06.01"
    }
}
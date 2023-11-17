package com.kt.apps.autoupdate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.os.bundleOf
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
    private var _appName: String? = null
    private var _appVersion: String? = null

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[AppUpdateViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            _appName = arguments?.getString(EXTRA_APP_NAME)
            _appVersion = arguments?.getString(EXTRA_VERSION_NAME) ?: appVersion
        } else {
            _appName = savedInstanceState.getString(EXTRA_APP_NAME)
            _appVersion = savedInstanceState.getString(EXTRA_VERSION_NAME) ?: appVersion
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        _appName?.let {
            binding.versionTitle.text = getString(R.string.app_version_title, _appName, _appVersion)
        } ?: kotlin.run {
            binding.versionTitle.text = getString(R.string.version_title, appVersion)
        }

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

        binding.imediaLink.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, FragmentQrCode.newInstance(binding.imediaLink.text.trim().toString()))
                .addToBackStack(null)
                .commit()
        }

        (binding.root as BrowseFrameLayout).setOnFocusSearchListener { focused, direction ->
            if (direction == View.FOCUS_LEFT) {
                return@setOnFocusSearchListener null
            }
            if (direction == View.FOCUS_DOWN) {
                when (focused) {
                    binding.btnCheckUpdate -> return@setOnFocusSearchListener binding.imediaLink
                    binding.imediaLink -> return@setOnFocusSearchListener binding.fbGroupLink
                    binding.fbGroupLink -> return@setOnFocusSearchListener binding.zaloGroupLink
                }
            } else if (direction == View.FOCUS_UP) {
                when (focused) {
                    binding.imediaLink -> return@setOnFocusSearchListener binding.btnCheckUpdate
                    binding.fbGroupLink -> return@setOnFocusSearchListener binding.imediaLink
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
                    jsonGroups.optString("imedia").takeIf {
                        it.isNotEmpty() && it.isNotBlank()
                    }?.let {
                        binding.imediaLink.text = it
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

    override fun onSaveInstanceState(outState: Bundle) {
        _appName?.let {
            outState.putString(EXTRA_APP_NAME, it)
        }
        _appVersion?.let {
            outState.putString(EXTRA_VERSION_NAME, it)
        }
        super.onSaveInstanceState(outState)

    }

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return BrowseSupportFragment.MainFragmentAdapter(this)
    }

    companion object {
        private const val EXTRA_VERSION_NAME = "extra:version_name"
        private const val EXTRA_APP_NAME = "extra:app_name"
        var appVersion: String = "23.06.01"
        fun newInstance(
            appName: String,
            versionName: String
        ) = FragmentInfo().apply {
            arguments = bundleOf().apply {
                putString(EXTRA_APP_NAME, appName)
                putString(EXTRA_VERSION_NAME, versionName)
            }
        }
    }
}
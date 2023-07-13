package com.kt.apps.autoupdate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.autoupdate.R
import com.kt.apps.autoupdate.databinding.FragmentInfoBinding
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.BrowseFrameLayout
import com.kt.apps.core.base.leanback.BrowseSupportFragment
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
    }

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return BrowseSupportFragment.MainFragmentAdapter(this)
    }

    companion object {
        var appVersion: String = "23.06.01"
    }
}
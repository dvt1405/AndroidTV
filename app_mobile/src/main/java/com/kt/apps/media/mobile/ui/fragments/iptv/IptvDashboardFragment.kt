package com.kt.apps.media.mobile.ui.fragments.iptv

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentIptvDashboardBinding
import com.kt.apps.media.mobile.ui.fragments.dashboard.adapter.IDashboardHelper
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.tv.adapter.TVDashboardAdapter
import com.kt.apps.media.mobile.ui.fragments.tvchannels.TVChannelsFragment
import com.kt.apps.media.mobile.utils.clicks
import com.kt.apps.media.mobile.viewmodels.IPTVViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class IptvDashboardFragment : BaseFragment<FragmentIptvDashboardBinding>() {
    @Inject lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.fragment_iptv_dashboard
    override val screenName: String
        get() = "IPTV Dashboard"

    private val viewModel by lazy {
        IPTVViewModel(ViewModelProvider(requireActivity(), factory))
    }

    private val motionLayout by lazy {
        binding.motionLayout
    }

    private val tabLayout by lazy {
        binding.tabLayout
    }

    private val list: MutableList<ExtensionsConfig> = mutableListOf()

    private val _adapter by lazy {
        object: FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return list.size
            }

            override fun createFragment(position: Int): Fragment {
                return  IptvChannelListFragment.newInstance(list.getOrNull(position)?.sourceUrl ?: "")
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tabLayout?.let {tabLayout ->
            binding.viewpager?.let {viewPager2 ->
                viewPager2.adapter = _adapter
                viewPager2.isUserInputEnabled = false
                val tab = TabLayoutMediator(
                    tabLayout, viewPager2, true, false
                ) { tab, position ->
                    tab.text = list.getOrNull(position)?.sourceName ?: ""
                }
                tab.attach()
            }
        }

        binding.removeExtension?.setOnClickListener {
            binding.tabLayout?.selectedTabPosition?.apply {
                list.getOrNull(this)?.apply {
                    showAlertRemoveExtension(this)
                }
            }
        }

    }

    override fun initAction(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            binding.addExtension?.clicks()?.collectLatest {
                showAddIPTVDialog()
            }
        }

        viewModel.addExtensionsConfig
            .onEach { viewModel.reloadData() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.extensionConfigs.collectLatest {
                if (it.isNotEmpty()) {
                    delay(250)
                    motionLayout?.transitionToState(R.id.end)
                    list.clear()
                    list.addAll(it)
                    _adapter.notifyDataSetChanged()
                } else {
                    motionLayout?.transitionToState(R.id.start)
                }
            }
        }
    }



    private fun showAddIPTVDialog() {
        val dialog = AddExtensionFragment()
        dialog.onSuccess = {
            it.dismiss()
        }
        dialog.show(childFragmentManager, AddExtensionFragment.TAG)
    }

    private fun showAlertRemoveExtension(config: ExtensionsConfig) {
        AlertDialog.Builder(context, R.style.AlertDialogTheme).apply {
            setMessage("Bạn có muốn xóa nguồn ${config.sourceName}?")
            setCancelable(false)
            setPositiveButton("Có") { dialog, which ->
//                deleteExtension(sourceName = sourceName)
                lifecycleScope.launch {
                    viewModel.remove(config)
                }
                dialog.dismiss()

            }
            setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
            }
        }
            .create()
            .show()

    }
}

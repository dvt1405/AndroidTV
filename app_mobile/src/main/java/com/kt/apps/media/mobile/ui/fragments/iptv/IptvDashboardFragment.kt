package com.kt.apps.media.mobile.ui.fragments.iptv

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.apps.core.base.BaseDialogFragment
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentIptvDashboardBinding
import com.kt.apps.media.mobile.ui.complex.ComplexActivity
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.utils.clicks
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.viewmodels.IPTVViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jsoup.Connection.Base
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

    private var pendingIndex: Int = -1

    private val _adapter by lazy {
        IPTVDashboardAdapter(this)
    }
    override fun initView(savedInstanceState: Bundle?) {
        tabLayout?.let {tabLayout ->
            binding.viewpager?.let {viewPager2 ->
                viewPager2.adapter = _adapter
                viewPager2.isUserInputEnabled = false
                val tab = TabLayoutMediator(
                    tabLayout, viewPager2, true, false
                ) { tab, position ->
                    tab.text = _adapter.getTitleForPage(position)
                    pendingIndex = binding.viewpager.currentItem
                }
                tab.attach()
            }
        }

        _adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (binding.viewpager.currentItem != pendingIndex) {
                    binding.viewpager.currentItem = pendingIndex
                }
            }
        })

        binding.removeExtension?.setOnClickListener {
            binding.tabLayout?.selectedTabPosition?.apply {
                _adapter.getItem(this)?.apply {
                    showAlertRemoveExtension(this)
                }
            }
        }

        pendingIndex = savedInstanceState?.getInt(currentIndex, -1) ?: -1
    }

    override fun initAction(savedInstanceState: Bundle?) {
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            launch {
                binding.addExtension.clicks().collectLatest {
                    showAddIPTVDialog()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.extensionConfigs
                .collectLatest {
                    Log.d(TAG, "viewModel.extensionConfigs: ${it} ${binding.viewpager.currentItem}")
                    if (it.isNotEmpty() && motionLayout.currentState != R.id.end) {
                            motionLayout?.transitionToState(R.id.end)
                    } else if(it.isEmpty() && motionLayout.currentState != R.id.start) {
                            motionLayout?.transitionToState(R.id.start)
                    }
                    _adapter.onRefresh(it)
                    delay(200)
                    if (pendingIndex >= 0) {
                        binding.viewpager.currentItem = pendingIndex
                    }
                }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(currentIndex, binding.viewpager.currentItem)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)


        lifecycleScope.launch {
            Log.d(TAG, "onViewStateRestored viewModel.extensionConfigs: ${this} ${binding.viewpager.currentItem}")
            savedInstanceState?.let {
                it.getInt(currentIndex, 0)
            }?.run {
                binding.viewpager.currentItem = this
                Log.d(TAG, "onViewStateRestored viewModel.extensionConfigs: ${this} ${binding.viewpager.currentItem}")
            }
        }
    }

    override fun onDestroyView() {
        binding.viewpager.adapter = null
        super.onDestroyView()
    }

    private fun showAddIPTVDialog() {
        val dialog = AddExtensionFragment()
        dialog.onSuccess = {
            it.dismiss()
        }

        dialog.show(childFragmentManager, AddExtensionFragment.TAG)
    }

    private fun showAlertRemoveExtension(config: ExtensionsConfig) {
        val dialog = RemoveIPTVDialogFragment.newInstance(config.sourceName)
        childFragmentManager.setFragmentResultListener(RemoveIPTVDialogFragment.TAG, this) {
            key, bundle ->
            when(bundle.getInt(RemoveIPTVDialogFragment.RESULT, 0)) {
                Activity.RESULT_OK -> {
                    lifecycleScope.launch {
                        viewModel.remove(config)
                        binding.viewpager.adapter = null
                        binding.viewpager.adapter = _adapter
                     }
                }
            }
         }
        dialog.show(childFragmentManager, RemoveIPTVDialogFragment.TAG)
    }

    companion object {
        private const val currentIndex = "CURRENT_INDEX"
        fun newInstance(): IptvDashboardFragment {
            val args = Bundle()

            val fragment = IptvDashboardFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

class RemoveIPTVDialogFragment: BaseDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context, R.style.AlertDialogTheme).apply {
            setMessage("Bạn có muốn xóa nguồn ${arguments?.getString(SOURCE_NAME)}?")
            setCancelable(true)
            setPositiveButton("Có") { dialog, which ->
                setFragmentResult(TAG, bundleOf(RESULT to Activity.RESULT_OK) )
                this@RemoveIPTVDialogFragment.dismiss()
            }
            setNegativeButton("Không") { dialog, _ ->
                this@RemoveIPTVDialogFragment.dismiss()
            }
        }
        return builder.create()
    }

    companion object {
        const val TAG = "RemoveIPTVDialogFragment"
        const val RESULT = "RESULT"
        const val SOURCE_NAME = "SOURCE_NAME"

        fun newInstance(sourceName: String): RemoveIPTVDialogFragment {
            val fragment =  RemoveIPTVDialogFragment().apply {
                arguments = bundleOf(SOURCE_NAME to sourceName)
            }
            return fragment
        }
    }
}
class IPTVDashboardAdapter(val fragment: Fragment) : FragmentStateAdapter(fragment){

    private val _listItem by lazy {
        mutableListOf<ExtensionsConfig>()
    }

    override fun getItemCount(): Int {
        return _listItem.size
    }


    override fun createFragment(position: Int): Fragment {
        return  IptvChannelListFragment.newInstance(_listItem.getOrNull(position)?.sourceUrl ?: "")
    }

    fun getItem(position: Int): ExtensionsConfig? {
        return _listItem.getOrNull(position)
    }

    fun getTitleForPage(position: Int): CharSequence {
        return _listItem.getOrNull(position)?.sourceName ?: ""
    }
    fun onRefresh(listItemCategory: List<ExtensionsConfig>) {
        _listItem.clear()
        _listItem.addAll(listItemCategory)
        notifyDataSetChanged()
    }
}


package com.kt.apps.media.xemtv.ui.playback

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.ClassPresenterSelector
import com.kt.apps.core.base.leanback.ItemBridgeAdapter
import com.kt.apps.core.base.leanback.ListRowPresenter
import com.kt.apps.core.databinding.LayoutTvProgramScheduleBinding
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.R
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.visible
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.extensions.ExtensionsViewModel
import javax.inject.Inject

class FragmentProgramSchedule : BaseFragment<LayoutTvProgramScheduleBinding>() {

    @Inject
    lateinit var factory: BaseViewModelFactory

    private val tvViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }

    override val layoutResId: Int
        get() = R.layout.layout_tv_program_schedule
    override val screenName: String
        get() = "FragmentProgramSchedule"

    private val adapter by lazy {
        ArrayObjectAdapter().apply {
            presenterSelector = ClassPresenterSelector()
                .addClassPresenter(ListRowPresenter::class.java, ListRowPresenter().apply {})
                .addClassPresenter(TVScheduler.Programme::class.java, ProgramSchedulePresenter())
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        val itemBridgeAdapter = ItemBridgeAdapter()
        itemBridgeAdapter.setAdapter(adapter)
        binding.verticalGridView.adapter = itemBridgeAdapter
    }

    override fun initAction(savedInstanceState: Bundle?) {
        binding.currentProgramTitle = tvViewModel.currentProgramTitle
        binding.verticalGridView.setOnChildSelectedListener { parent, view, position, id ->
            if (null != view && position >= 0) {
                view.requestFocus()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        extensionsViewModel.listProgramForChannel.observe(viewLifecycleOwner) {
            handleListProgram(it)
        }
        tvViewModel.listProgramForChannel.observe(viewLifecycleOwner) {
            handleListProgram(it)
        }
    }

    private fun handleListProgram(dataState: DataState<List<TVScheduler.Programme>>) {
        when (dataState) {
            is DataState.Success -> {
                adapter.clear()
                if (dataState.data.isEmpty()) {
                    binding.emptyProgramText.visible()
                    return
                }
                binding.emptyProgramText.gone()
                adapter.addAll(adapter.size(), dataState.data)
                dataState.data.indexOfFirst { item ->
                    item.isCurrentProgram()
                }.takeIf {
                    it >= 0
                }?.let { index ->
                    binding.verticalGridView.selectedPosition = index
                }
            }

            is DataState.Error -> {
                adapter.clear()
                binding.emptyProgramText.visible()
            }

            else -> {

            }
        }
    }

    companion object {
        fun newInstance() = FragmentProgramSchedule().apply {

        }
    }
}
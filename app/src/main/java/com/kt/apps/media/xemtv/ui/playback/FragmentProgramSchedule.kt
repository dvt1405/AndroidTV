package com.kt.apps.media.xemtv.ui.playback

import android.os.Bundle
import android.util.Log
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
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import javax.inject.Inject

class FragmentProgramSchedule : BaseFragment<LayoutTvProgramScheduleBinding>() {

    @Inject
    lateinit var factory: BaseViewModelFactory

    private val tvViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    override val layoutResId: Int
        get() = R.layout.layout_tv_program_schedule
    override val screenName: String
        get() = "FragmentProgramSchedule"

    private val adapter by lazy {
        ArrayObjectAdapter().apply {
            presenterSelector = ClassPresenterSelector()
                .addClassPresenter(ListRowPresenter::class.java, ListRowPresenter().apply {})
                .addClassPresenter(TVScheduler.Programme::class.java, ProgramSchedulePresenter().apply {
//                    val itemAlignment = ItemAlignmentFacet()
//                    val def = ItemAlignmentFacet.ItemAlignmentDef()
//                    def.itemAlignmentOffset = 0
//                    def.itemAlignmentOffsetPercent = 50f
//                    itemAlignment.alignmentDefs = arrayOf(def)
//                    this.setFacet(ItemAlignmentFacet::class.java, itemAlignment)
                })
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        val itemBridgeAdapter = ItemBridgeAdapter()
        itemBridgeAdapter.setAdapter(adapter)
//        FocusHighlightHelper.setupBrowseItemFocusHighlight(
//            itemBridgeAdapter,
//            FocusHighlight.ZOOM_FACTOR_MEDIUM,
//            false
//        )
        binding.verticalGridView.setOnChildSelectedListener { parent, view, position, id ->
            Log.e("TAG", "view: $view, Position: $position")
            view.requestFocus()
        }
        binding.verticalGridView.adapter = itemBridgeAdapter
    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvViewModel.listProgramForChannel.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    adapter.clear()
                    adapter.addAll(adapter.size(), it.data)
                    it.data.indexOfLast { item ->
                        item.startTimeMilli <= System.currentTimeMillis()
                    }.takeIf {
                        it >= 0
                    }?.let { index ->
                        binding.verticalGridView.selectedPosition = index
                    }
                }

                is DataState.Error -> {

                }

                else -> {

                }
            }
        }
    }
}
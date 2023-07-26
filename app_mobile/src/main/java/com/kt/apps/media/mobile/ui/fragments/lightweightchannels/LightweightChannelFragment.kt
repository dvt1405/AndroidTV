package com.kt.apps.media.mobile.ui.fragments.lightweightchannels

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentLightweightChannelBinding
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.apps.media.mobile.utils.groupAndSort
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class LightweightChannelFragment : BaseFragment<FragmentLightweightChannelBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_lightweight_channel
    override val screenName: String
        get() = "Lightweight channel"

    override fun initView(savedInstanceState: Bundle?) {

    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val recyclerView by lazy {
        binding.channelList
    }

//    private val adapter by lazy {
//        LightweightChannelAdapter().apply {
//            onChildItemClickListener = { item, _ ->
//                when (item) {
//                    is ChannelElement.TVChannelElement -> tvChannelViewModel?.loadLinkStreamForChannel(
//                        item.model
//                    )
//                    else -> {}
//                }
//            }
//        }
//    }

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
        }
    }

    private val _tvChannelData by lazy {
        MutableStateFlow<List<TVChannel>>(emptyList())
    }


    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel?.apply {
            tvChannelLiveData.observe(this@LightweightChannelFragment) {
                when (it) {
                    is DataState.Success -> _tvChannelData.value = it.data
                    else  -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                _tvChannelData.collectLatest {
//                    delay(500)
                    reloadOriginalSource(it)
                }
            }
        }
    }

    private fun reloadOriginalSource(data: List<TVChannel>) {
//        val grouped = groupAndSort(data).map {
//            Pair(
//                it.first,
//                it.second.map { tvChannel -> ChannelElement.TVChannelElement(tvChannel) })
//        }
//        adapter.onRefresh(grouped)
        recyclerView.reloadAllData(data.map { ChannelElement.TVChannelElement(it) })
    }

    companion object {
        fun newInstance() = LightweightChannelFragment()
    }
}
package com.kt.apps.media.mobile.ui.fragments.favorite

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.inVisible
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentFavoriteBinding
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.view.ChannelListData
import com.kt.apps.media.mobile.ui.view.childItemClicks
import com.kt.apps.media.mobile.utils.ActivityIndicator
import com.kt.apps.media.mobile.utils.groupAndSort
import com.kt.apps.media.mobile.utils.repeatLaunchesOnLifeCycle
import com.kt.apps.media.mobile.utils.trackJob
import com.kt.apps.media.mobile.viewmodels.FavoriteInteractor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class FavoriteFragment : BaseFragment<FragmentFavoriteBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_favorite
    override val screenName: String
        get() = "FavoriteFragment"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val interactor: FavoriteInteractor by lazy {
        FavoriteInteractor(ViewModelProvider(requireActivity(), factory), viewLifecycleOwner.lifecycleScope)
    }
    private val activityIndicator = ActivityIndicator()
    override fun initView(savedInstanceState: Bundle?) {
        binding.progressBarContainer.inVisible()
    }

    override fun initAction(savedInstanceState: Bundle?) {
        repeatLaunchesOnLifeCycle(Lifecycle.State.CREATED) {
            lifecycleScope.launch {
                activityIndicator.isLoading.collectLatest {
                    binding.progressBarContainer.visibility = if (it) View.VISIBLE else View.GONE
                }
            }
        }

        repeatLaunchesOnLifeCycle(Lifecycle.State.STARTED) {
            lifecycleScope.launch {
                interactor.loadFavorites()
            }.trackJob(activityIndicator)

            lifecycleScope.launch {
                interactor.listFavorite.collectLatest {
                    if (it.isEmpty()) {
                        showPlaceholderView()
                    } else {
                        showContentView()
                    }

                    binding.channelList.reloadAllData(groupAndSort(it).map { pair -> ChannelListData(pair.first, pair.second.map { video ->
                        ChannelElement.FavoriteVideo(video)
                    }) })

                }
            }

            lifecycleScope.launch {
                binding.channelList.childItemClicks()
                    .collectLatest {
                        lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->
                            showErrorDialog(content = getString(R.string.error_happen))
                        }) {
                            interactor.openPlayback(it.data)
                        }.trackJob(activityIndicator)
                    }
            }
        }
    }

    private fun showContentView() {
        if (binding.viewSwitcher.nextView.id == R.id.channel_list) {
            binding.viewSwitcher.showNext()
        }
    }

    private fun showPlaceholderView() {
        if (binding.viewSwitcher.nextView.id != R.id.channel_list) {
            binding.viewSwitcher.showNext()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FavoriteFragment()
    }
}
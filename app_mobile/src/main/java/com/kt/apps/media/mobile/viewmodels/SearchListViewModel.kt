package com.kt.apps.media.mobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.fragments.playback.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.asProgressFlow
import com.kt.apps.media.mobile.utils.asUpdateFlow
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class SearchListViewModel(private val provider: ViewModelProvider): IUIControl {
    private val searchViewModel by lazy {
        provider[SearchViewModels::class.java]
    }


    override val playbackViewModel: PlaybackViewModel by lazy {
        provider[PlaybackViewModel::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    private var _isProgressing = MutableStateFlow(false)
    val isProgressing
        get() = combine(
            _isProgressing,
            searchProgress,
            uiControlViewModel.voiceSearchProgressing
        ) { t1, t2, t3 -> t1 || t2 || t3}

   val searchResult: Flow<Map<String, List<SearchForText.SearchResult>>> by lazy {
       searchViewModel.searchQueryLiveData
           .asUpdateFlow(tag = "SearchList")
   }


    val searchProgress: Flow<Boolean> by lazy {
        searchViewModel.searchQueryLiveData
            .asProgressFlow()
    }
    suspend fun openPlayback(data: IChannelElement) {
        try {
            val searchData = when(data) {
                is ChannelElement.SearchTV -> data.model
                is ChannelElement.SearchExtension -> data.model
                is ChannelElement.SearchHistory -> data.model
                else -> null
            } ?: return

            _isProgressing.emit(true)

            searchViewModel.getResultForItem(searchData, "")
            val result = searchViewModel.selectedItemLiveData
                .await()

            when(result) {
                is TVChannelLinkStream -> {
                    val channel = result.channel
                    if (channel.isRadio)
                        PrepareStreamLinkData.Radio(channel)
                    else
                        PrepareStreamLinkData.TV(channel)
                }
                is ExtensionsChannelAndConfig -> {
                    PrepareStreamLinkData.IPTV(result.channel, result.config.sourceUrl)
                }

                else -> null
            }?.run {
                openPlayback(this)
            }
        }
        catch (t: Throwable) {
            Log.d(TAG, "openPlayback: $t")
        }
        finally {
            _isProgressing.emit(false)
        }

    }
}
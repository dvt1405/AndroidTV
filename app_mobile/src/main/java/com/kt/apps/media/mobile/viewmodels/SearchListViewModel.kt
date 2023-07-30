package com.kt.apps.media.mobile.viewmodels

import android.text.SpannableString
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsChannelAndConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.media.mobile.models.PrepareStreamLinkData
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.utils.asFlow
import com.kt.apps.media.mobile.utils.await
import com.kt.apps.media.mobile.utils.toExtensionChannel
import com.kt.apps.media.mobile.viewmodels.features.IUIControl
import com.kt.apps.media.mobile.viewmodels.features.SearchViewModels
import com.kt.apps.media.mobile.viewmodels.features.UIControlViewModel
import com.kt.apps.media.mobile.viewmodels.features.openPlayback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

class SearchListViewModel(private val provider: ViewModelProvider): IUIControl {
    private val searchViewModel by lazy {
        provider[SearchViewModels::class.java]
    }

    override val uiControlViewModel: UIControlViewModel by lazy {
        provider[UIControlViewModel::class.java]
    }

    private var _isProgressing = MutableStateFlow(false)
    val isProgressing
        get() = _isProgressing.asStateFlow()
   val searchResult: Flow<Map<String, List<SearchForText.SearchResult>>>
        get() = searchViewModel.searchQueryLiveData
            .asFlow()
            .catch { emit(emptyMap()) }

    suspend fun openPlayback(data: IChannelElement) {
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
        delay(5000)
        _isProgressing.emit(false)

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
}
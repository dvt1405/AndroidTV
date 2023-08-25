package com.kt.apps.core.tv.datasource.impl

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.needRefreshData
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.*
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Observable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@TVScope
class SCTVDataSourceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val firebaseDataBase: FirebaseDatabase,
    private val keyValueStorage: TVStorage,
    private val roomDataBase: RoomDataBase,
    private val remoteConfig: FirebaseRemoteConfig,
) : ITVDataSource {
    companion object {
        private const val WEB_PAGE_BASE_URL = "https://sctvonline.vn/"
        private const val BACKEND_BASE_URL = "https://apicdn.sctvonline.vn/"
        private const val MAIN_PAGE_MENU = "backend/cm/menu/sctv-mobile/"
        private const val REFERER = "https://sctvonline.vn/"
        private const val TENANTS = "tenants/sctv/"

        private const val PATH_QUERY_PAGES_MENU = "tenant_pages/" +
                "{menu_page_id}/ribbons/" +
                "?apply_filter_for_side_navigation_section=true" +
                "&limit=50&select="
        private const val SELECT_QUERY_PAGES_FOR_MENU_ID =
            "{\"Content\":" +
                    "[\"id\",\"slug\",\"has_free_content\"," +
                    "\"is_new_release\"," +
                    "\"is_premium\",\"has_free_content\",\"content_categories\"," +
                    "\"total_episodes\",\"released_episode_count\",\"images\",\"title\"," +
                    "\"video_source\",\"type\",\"top_index\",\"min_sub_tier\"]," +
                    "\"Banner\":[" +
                    "\"num_first_episode_preview\",\"slug\",\"id\"," +
                    "\"is_premium\",\"type\",\"is_watchable\",\"has_free_content\"," +
                    "\"long_description\",\"short_description\",\"title\"," +
                    "\"has_free_content\",\"images\",\"min_sub_tier\"" +
                    "]," +
                    "\"RibbonDetail\":[" +
                    "\"display_type\",\"id\",\"items\",\"name\",\"odr\",\"show_flag_odr\"," +
                    "\"slug\",\"type\",\"is_visible_in_ribbon_main_section\"," +
                    "\"is_default_display\",\"min_sub_tier\"" +
                    "]}"

        private const val PATH_QUERY_CHANNEL_DETAIL = "${TENANTS}contents/{channel_slug}/view?select="
        private const val SELECT_QUERY_CHANEL_DETAIL_VALUE =
            "{\"Content\":[\"current_season\",\"id\",\"slug\"," +
                    "\"is_watchable\",\"progress\",\"youtube_video_id\"," +
                    "\"link_play\",\"play_info\",\"payment_infors\"," +
                    "\"is_favorite\",\"drm_session_info\"]}"
    }

    private val listRadioGroupSupport: List<TVChannelGroup> by lazy {
        listOf(TVChannelGroup.VOV, TVChannelGroup.VOH)
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        val source1 = getTVChannelPageForMenu()
            .map { listTVWithCategory ->
                val listItems = mutableListOf<TVChannel>()
                listTVWithCategory.forEach { channelCategory ->
                    channelCategory.items.forEach { channelItem ->
                        val tvChannel = TVChannel(
                            tvGroup = channelCategory.name,
                            tvChannelWebDetailPage = "${WEB_PAGE_BASE_URL}detail/${channelItem.slug}",
                            tvChannelName = channelItem.title,
                            logoChannel = channelItem.images.thumbnail,
                            sourceFrom = TVDataSourceFrom.SCTV.name,
                            channelId = channelItem.slug
                        )
                        listItems.add(tvChannel)
                    }
                }
                listItems
            }

        val source2 = Observable.create { emitter ->
            val listGroup = listRadioGroupSupport.map {
                it.name
            }
            val totalChannel = mutableListOf<TVChannel>()
            var count = 0
            var isOnline: Boolean = false
            val needRefresh = this.needRefreshData(remoteConfig, keyValueStorage)
            listGroup.forEach { group ->
                if (keyValueStorage.getTvByGroup(group).isNotEmpty() && !needRefresh) {
                    isOnline = false
                    totalChannel.addAll(keyValueStorage.getTvByGroup(group))
                    count++
                    if (count == listGroup.size) {
                        if (emitter.isDisposed) return@create
                        emitter.onNext(totalChannel)
                        emitter.onComplete()
                    }
                } else {
                    isOnline = true
                    fetchTvList(group) {
                        keyValueStorage.saveTVByGroup(group, it)
                        totalChannel.addAll(it)
                        count++
                        if (count == listGroup.size) {
                            if (emitter.isDisposed) return@fetchTvList
                            emitter.onNext(totalChannel)
                            if (needRefresh) {
                                keyValueStorage.saveRefreshInVersion(
                                    Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
                                    remoteConfig.getLong(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)
                                )
                            }
                            emitter.onComplete()
                        }
                    }.addOnFailureListener {
                        count++
                        if (emitter.isDisposed) return@addOnFailureListener
                        emitter.onError(it)
                    }
                }
            }
        }
        return Observable.concat(source1, source2)
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {

        val channelSlug = if (tvChannel.sourceFrom == TVDataSourceFrom.SCTV.name) {
            tvChannel.channelId
        } else {
            try {
                tvChannel.urls.last {
                    it.dataSource == "sctv"
                }.url.toHttpUrl()
                    .pathSegments.last {
                        it.isNotBlank()
                    }
            }catch (e: NoSuchElementException) {
                return Observable.error(Throwable("Not url support SCTV source"))
            }
        }

        return Observable.create {
            val response = try {
                trustEveryone()
                okHttpClient.newCall(
                    Request.Builder()
                        .header("origin", "https://sctvonline.vn")
                        .header("referer", REFERER)
                        .url(
                            "$BACKEND_BASE_URL${
                                PATH_QUERY_CHANNEL_DETAIL.replace(
                                    "{channel_slug}",
                                    channelSlug
                                )
                            }$SELECT_QUERY_CHANEL_DETAIL_VALUE"
                        )
                        .build()
                ).execute()
            } catch (e: Exception) {
                if (it.isDisposed) return@create
                it.onError(e)
                return@create
            }

            if (response.code in 200..299) {
                val json = JSONObject(response.body.string())
                if (it.isDisposed) {
                    return@create
                }
                val linkPlay = json.optString("link_play")

                val hlsInplayInfo = json.optJSONObject("play_info")
                    ?.optJSONObject("data")
                    ?.optString("hls_link_play")
                Logger.d(this, "playInfo", message = hlsInplayInfo ?: "Empty")
                it.onNext(
                    TVChannelLinkStream(
                        tvChannel.apply {
                            this.referer = WEB_PAGE_BASE_URL
                        },
                        if (hlsInplayInfo.isNullOrEmpty()) {
                            listOf(linkPlay)
                        } else {
                            listOf(linkPlay, hlsInplayInfo)
                        }
                    )
                )
                it.onComplete()
            } else {
                if (it.isDisposed) {
                    return@create
                }
                val streamingLink = tvChannel.urls.filter {
                    it.type == "streaming"
                }
                if (streamingLink.isNotEmpty()) {
                    it.onError(Throwable(response.message))
                } else {
                    it.onNext(
                        TVChannelLinkStream(
                            tvChannel.apply {
                                this.referer = WEB_PAGE_BASE_URL
                            },
                            streamingLink.map {
                                it.url
                            }
                        )
                    )
                    it.onComplete()
                }
            }
        }.retry(3)
    }

    private fun getTVChannelPageForMenu(menuId: String = "truyen-hinh-ecb1ec92"): Observable<List<SCTVPages.Ribbon>> {
        return getMainPageMenu(2)
            .map {
                val id = it.first {
                    it.slug == menuId
                }.id
                val url = "$BACKEND_BASE_URL$TENANTS${
                    PATH_QUERY_PAGES_MENU.replace(
                        "{menu_page_id}", id
                    )
                }$SELECT_QUERY_PAGES_FOR_MENU_ID"
                url
            }
            .flatMap {
                val response = okHttpClient.newCall(
                    Request.Builder()
                        .header("origin", "https://sctvonline.vn")
                        .header("referer", REFERER)
                        .url(it)
                        .build()
                )
                    .execute()

                val code = response.code
                val headers = response.headers
                if (code in 200..299) {
                    Observable.just(
                        Gson().fromJson(response.body.string(), SCTVPages::class.java)
                            .ribbons
                    )
                } else {
                    Observable.error(Throwable("canretry"))
                }
            }
    }
    private fun fetchTvList(
        name: String,
        onComplete: (list: List<TVChannel>) -> Unit
    ): Task<DataSnapshot> {
        return firebaseDataBase.reference.child(name)
            .get()
            .addOnSuccessListener {
                val value = it.getValue<List<DataFromFirebase?>>() ?: return@addOnSuccessListener
                onComplete(
                    value.filterNotNull().map { dataFromFirebase ->
                        TVChannel(
                            name,
                            tvChannelName = dataFromFirebase.name,
                            tvChannelWebDetailPage = dataFromFirebase.url,
                            logoChannel = dataFromFirebase.logo,
                            sourceFrom = TVDataSourceFrom.V.name,
                            channelId = if (name in listOf(TVChannelGroup.VOV.name, TVChannelGroup.VOH.name)) {
                                dataFromFirebase.name.removeAllSpecialChars()
                            } else {
                                dataFromFirebase.url.trim().removeSuffix("/").split("/").last()
                            }
                        )
                    })
            }
    }

    fun getMainPageMenu(retryCount: Int): Observable<List<SCTVMainMenu.Item>> {
        val totalRetryCount = AtomicInteger(retryCount)
        val observableSource: Observable<SCTVMainMenu> = Observable.create {
            val mainPageUrl = "$BACKEND_BASE_URL$MAIN_PAGE_MENU"

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url(mainPageUrl)
                    .header("origin", "https://sctvonline.vn")
                    .header("referer", REFERER)
                    .build()
            ).execute()

            if (response.code in 200..299) {
                val body = response.body.string()
                val listMenu = Gson().fromJson(body, SCTVMainMenu::class.java)
                if (it.isDisposed) {
                    return@create
                }
                it.onNext(listMenu)
                it.onComplete()
            } else {
                if (it.isDisposed) {
                    return@create
                }
                it.tryOnError(Throwable("canretry"))

            }
        }.retryWhen {
            return@retryWhen it.takeWhile {
                it.message == "canretry" && totalRetryCount.decrementAndGet() >= 0
            }
        }
        return observableSource.map {
            it.toList()
        }
    }

    class SCTVMainMenu : ArrayList<SCTVMainMenu.Item>() {
        data class Item(
            val banner_style: String,
            val color_one: String,
            val color_two: String,
            val display_style: String,
            val icon: String,
            val id: String,
            val name: String,
            val page_options: PageOptions,
            val required: Boolean,
            val slug: String,
        ) {
            data class PageOptions(
                val contain_sub_item: Boolean,
                val content_navigation_option: String
            )
        }
    }

    class SCTVPages(
        val banner_style: String,
        val display_style: String,
        val name: String,
        val page_options: PageOptions,
        val ribbons: List<Ribbon>,
    ) {
        data class PageOptions(
            val contain_sub_item: Boolean,
            val content_navigation_option: String
        )

        data class Ribbon(
            val display_type: Int,
            val id: String,
            val is_default_display: Boolean,
            val is_visible_in_ribbon_main_section: Boolean,
            val items: List<Item>,
            val name: String,
            val odr: Int,
            val show_flag_odr: Boolean,
            val slug: String,
            val type: Int
        )

        data class Item(
            val content_categories: List<ContentCategory>,
            val has_free_content: Boolean,
            val hide_from_top_contents: Boolean,
            val id: String,
            val images: Images,
            val is_new_release: Boolean,
            val is_premium: Boolean,
            val released_episode_count: Int,
            val slug: String,
            val title: String,
            val top_index: Int,
            val total_episodes: Int,
            val type: Int,
            val video_source: Int
        )

        data class ContentCategory(
            val id: String,
            val name: String,
            val slug: String
        )

        data class Images(
            val backdrop: String,
            val banner: String,
            val banner_190_67_ratio: String,
            val banner_19_6_ratio: String,
            val banner_movie: String,
            val banner_tv_show: String,
            val channel_logo: String,
            val channel_wide_logo: String,
            val poster: String,
            val poster_banner: String,
            val rectangle_banner: String,
            val thumbnail: String,
            val thumbnail_9_5_ratio: String,
            val title_image: String
        )
    }


}
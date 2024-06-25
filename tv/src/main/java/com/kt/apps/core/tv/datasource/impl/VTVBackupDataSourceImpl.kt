package com.kt.apps.core.tv.datasource.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.ChannelSourceConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern
import javax.inject.Inject

class VTVBackupDataSourceImpl @Inject constructor(
    private val context: Context,
    private val dataBase: RoomDataBase,
    private val sharePreference: TVStorage,
    private val client: OkHttpClient
) : ITVDataSource {

    private val _cookie: MutableMap<String, String>

    private val config: ChannelSourceConfig by lazy {
        ChannelSourceConfig(
            baseUrl = "https://vtvgo.vn/",
            mainPagePath = "trang-chu.html",
            getLinkStreamPath = "ajax-get-stream"
        )
    }


    init {
        val cacheCookie = sharePreference.cacheCookie(TVDataSourceFrom.VTV_BACKUP)
        _cookie = cacheCookie.toMutableMap()
    }

    override fun getTvList(): Observable<List<TVChannel>> {
        val homepage = "${config.baseUrl.removeSuffix("/")}/${config.mainPagePath}"
        trustEveryone()
        return Observable.create { emitter ->
            val document = try {
                Jsoup.connect(homepage)
                    .cookies(_cookie)
                    .execute()
            } catch (e: Exception) {
                if (emitter.isDisposed) {
                    return@create
                }
                Firebase.crashlytics.recordException(e)
                if (e is IOException) {
                    emitter.onError(e)
                } else {
                    emitter.onError(Throwable("Error when connect to $homepage"))
                }
                return@create
            }
            _cookie.clear()
            _cookie.putAll(document.cookies())
            val body = document.parse().body()
            val listChannelDetail = mutableListOf<TVChannel>()
            body.getElementsByClass("list_channel")
                .forEach {
                    val detail = it.getElementsByTag("a").first()
                    val link = detail!!.attr("href")
                    val name = detail.attr("alt")
                    val logo = detail.getElementsByTag("img").first()!!.attr("src")
                    val regex = "[*?<=vtv\\d]*?(\\d+)"
                    val pattern = Pattern.compile(regex)
                    val matcher = pattern.matcher(link)
                    val listMatcher = mutableListOf<String>()
                    while (matcher.find()) {
                        matcher.group(0)?.let { it1 -> listMatcher.add(it1) }
                    }
                    var channelId: String? = null
                    if (listMatcher.isNotEmpty()) {
                        channelId = try {
                            listMatcher[1]
                        } catch (e: Exception) {
                            name.lowercase().replace("[^\\dA-Za-z ]", "")
                                .replace("\\s+", "+")
                                .lowercase()
                                .removeSuffix("hd")
                                .trim()
                        }
                    }
                    val channel = TVChannel(
                        tvGroup = TVChannelGroup.VTV.name,
                        logoChannel = logo,
                        tvChannelName = name,
                        tvChannelWebDetailPage = link,
                        sourceFrom = TVDataSourceFrom.VTV_BACKUP.name,
                        channelId = channelId ?: name.replace(" ", "")
                            .lowercase()
                            .removeSuffix("hd")
                    )
                    listChannelDetail.add(channel)
                }
            emitter.onNext(listChannelDetail)
            insertToDb(listChannelDetail)
            emitter.onComplete()

        }


    }

    private fun insertToDb(listChannelDetail: MutableList<TVChannel>) {
        sharePreference.saveTVByGroup(TVDataSourceFrom.VTV_BACKUP.name, listChannelDetail)
        CompositeDisposable()
            .add(dataBase.mapChannelDao()
                .insert(
                    listChannelDetail.map {
                        MapChannel(
                            channelId = it.channelId,
                            channelName = it.tvChannelName,
                            channelGroup = it.tvGroup,
                            fromSource = it.sourceFrom
                        )
                    }
                ).subscribe({}, {})
            )
    }

    override fun getTvLinkFromDetail(tvChannel: TVChannel, isBackup: Boolean): Observable<TVChannelLinkStream> {
        trustEveryone()
        Logger.d(this@VTVBackupDataSourceImpl, message = "getTvLinkFromDetail: $tvChannel")
        return Observable.create { emitter ->
            getLinkStream(tvChannel, {
                if (emitter.isDisposed) {
                    return@getLinkStream
                }
                emitter.onNext(it)
                emitter.onComplete()
            }) {
                if (emitter.isDisposed) {
                    return@getLinkStream
                }
                emitter.onError(it)
            }
        }
    }

    internal class GetHtml() {
        var html: String? = null
        var onComplete: () -> Unit = {}
        @JavascriptInterface
        fun getHtmPage(html: String) {
            this.html = html
            onComplete()
        }
    }

    private fun getLinkStream(
        channelTvDetail: TVChannel,
        onSuccess: (data: TVChannelLinkStream) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        val countDownLatch = CountDownLatch(1)
        val htmlCheck = GetHtml()
        Handler(Looper.getMainLooper()).post {
            val view = WebView(context)
            htmlCheck.onComplete = {
                countDownLatch.countDown()
                mMainHandler.post {
                    view.onPause()
                    view.destroy()
                }
            }
            view.addJavascriptInterface(htmlCheck, "HtmlCheck")
            view.settings.javaScriptEnabled = true
            view.settings.userAgentString = Constants.USER_AGENT
            view.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                }
            }
            view.webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Logger.d(this@VTVBackupDataSourceImpl, message = "onPageFinished: $url")
                    view?.loadUrl("javascript:window.HtmlCheck.getHtmPage('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                }
            }
            view.loadUrl(channelTvDetail.tvChannelWebDetailPage)
        }
        countDownLatch.await()
        val body = Jsoup.parse(htmlCheck.html)
        val script = body.getElementsByTag("script")
        for (it in script) {
            val html = it.html().trim()
            if (html.contains("token")) {
                val token: String? = getVarFromHtml("token", html)
                val id = getVarNumberFromHtml("id", html)
                val typeId: String? = getVarFromHtml("type_id", html)
                val time: String? = getVarFromHtml("time", html)
                if (anyNotNull(token, id, typeId, time)) {
                    getStream(
                        channelTvDetail,
                        token!!,
                        id!!,
                        typeId!!,
                        time!!,
                        onSuccess,
                        onError
                    )
                    break
                }
            }
        }
    }

    private fun getStream(
        kenhTvDetail: TVChannel,
        token: String,
        id: String,
        typeId: String,
        time: String,
        onSuccess: (data: TVChannelLinkStream) -> Unit,
        onError: (t: Throwable) -> Unit
    ) {
        val bodyRequest = FormBody.Builder()
            .add("type_id", typeId)
            .add("id", id)
            .add("time", time)
            .add("token", token)
            .build()
        val url = "${config.baseUrl}${config.getLinkStreamPath}"
        val request = Request.Builder()
            .url(url)
            .post(bodyRequest)
            .header("cookie", CookieManager.getInstance()
                .getCookie(config.baseUrl))
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("sec-fetch-site", "same-origin")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("origin", config.baseUrl)
            .header("referer", kenhTvDetail.tvChannelWebDetailPage.toHttpUrl().toString())
            .header("user-agent", Constants.USER_AGENT)
            .header("accept-encoding", "application/json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    try {
                        val vtvStream = Gson().fromJson(it, VtvStream::class.java)
                        onSuccess(
                            TVChannelLinkStream(
                                channel = kenhTvDetail,
                                linkStream = vtvStream.stream_url.map {
                                    TVChannel.Url.fromUrl(
                                        url = it,
                                        referer = kenhTvDetail.tvChannelWebDetailPage,
                                        origin = kenhTvDetail.tvChannelWebDetailPage.getBaseUrl()
                                    )
                                }
                            )
                        )
                    } catch (e: Exception) {
                        onError(e)
                    }

                } ?: onError(Throwable("Null body"))
            }

        })
    }

    private fun buildCookie(): String {
        val cookieBuilder = StringBuilder()
        for (i in _cookie.entries) {
            cookieBuilder.append(i.key)
                .append("=")
                .append(i.value)
                .append(";")
                .append(" ")
        }
        return cookieBuilder.toString().trim().removeSuffix(";")
    }

    private fun getVarFromHtml(name: String, text: String): String? {
        val regex = "(?<=var\\s$name\\s=\\s\').*?(?=\')"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        var value: String? = null

        while (matcher.find()) {
            value = matcher.group(0)
        }
        return value
    }

    private fun getVarNumberFromHtml(name: String, text: String): String? {
        val regex = "(?<=var\\s$name\\s=\\s)(\\d+)"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            return matcher.group(0)
        }
        return null
    }

    private fun anyNotNull(vararg variable: Any?): Boolean {
        for (v in variable) {
            if (v == null) return false
        }
        return true
    }

    companion object {
        private val mMainHandler = Handler(Looper.getMainLooper())
    }

}

data class VtvStream(
    val ads_tags: String,
    val ads_time: String,
    val channel_name: String,
    val chromecast_url: String,
    val content_id: Int,
    val date: String,
    val geoname_id: String,
    val is_drm: Boolean,
    val player_type: String,
    val remoteip: String,
    val stream_info: String,
    val stream_url: List<String>
)
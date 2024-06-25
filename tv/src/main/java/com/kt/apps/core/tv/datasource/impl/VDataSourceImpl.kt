package com.kt.apps.core.tv.datasource.impl

import android.util.Log
import androidx.core.os.bundleOf
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.tv.FirebaseLogUtils
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.needRefreshData
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.DataFromFirebase
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.removeAllSpecialChars
import io.jsonwebtoken.Jwts
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject


@TVScope
class VDataSourceImpl @Inject constructor(
    private val compositeDisposable: DisposableContainer,
    private val keyValueStorage: TVStorage,
    private val roomDataBase: RoomDataBase,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseDatabase: FirebaseDatabase,
    private val httpClient: OkHttpClient
) : ITVDataSource {
    private fun getSupportTVGroup(): List<TVChannelGroup> {
        return listOf(
            TVChannelGroup.VTV,
            TVChannelGroup.HTV,
            TVChannelGroup.VTC,
            TVChannelGroup.HTVC,
            TVChannelGroup.THVL,
            TVChannelGroup.DiaPhuong,
            TVChannelGroup.AnNinh,
            TVChannelGroup.VOV,
            TVChannelGroup.VOH,
            TVChannelGroup.Intenational
        )
    }

    private val _needRefresh: Boolean
        get() = this.needRefreshData(remoteConfig, keyValueStorage)

    private var isOnline: Boolean = false
    private val apiUrl = "https://api.vieon.vn/backend/cm/v5/slug/livetv/detail?platform=web&ui=012021"

    override fun getTvList(): Observable<List<TVChannel>> {
        val listGroup = getSupportTVGroup().map {
            it.name
        }

        return Observable.create<List<TVChannel>> { emitter ->
            val totalChannel = mutableListOf<TVChannel>()
            var count = 0
            val needRefresh = _needRefresh
            listGroup.forEach { group ->
                if (keyValueStorage.getTvByGroup(group).isNotEmpty() && !needRefresh) {
                    isOnline = false
                    totalChannel.addAll(keyValueStorage.getTvByGroup(group))
                    saveToRoomDB(group, keyValueStorage.getTvByGroup(group))
                    count++
                    if (count == listGroup.size) {
                        emitter.onNext(totalChannel)
                        emitter.onComplete()
                    }
                } else {
                    isOnline = true
                    fetchTvList(group) {
                        keyValueStorage.saveTVByGroup(group, it)
                        saveToRoomDB(group, it)
                        totalChannel.addAll(it)
                        count++
                        if (count == listGroup.size) {
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
                        emitter.onError(it)
                    }
                }
            }

        }.doOnError {
            FirebaseLogUtils.logGetListChannelError(TVDataSourceFrom.V.name, it)
        }.doOnComplete {
            FirebaseLogUtils.logGetListChannel(
                TVDataSourceFrom.V.name,
                bundleOf("fetch_from" to if (isOnline) "online" else "offline")
            )
        }
    }

    private fun saveToRoomDB(source: String, tvDetails: List<TVChannel>) {
        compositeDisposable.add(
            roomDataBase.mapChannelDao()
                .insert(
                    tvDetails.map {
                        val id = it.tvChannelWebDetailPage
                            .trim()
                            .removeSuffix("/")
                            .split("/")
                            .last()
                        MapChannel(
                            channelId = id,
                            channelName = it.tvChannelName,
                            fromSource = TVDataSourceFrom.V.name,
                            channelGroup = source
                        )
                    }
                )
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                })
        )
    }
    private var cookie = mutableMapOf<String, String>()

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        return Observable.create { emitter ->
            Logger.d(this@VDataSourceImpl, message = "getTvLinkFromDetail")
            val body = try {
                if (cookie.isEmpty()) {
                    cookie.putAll(Jsoup.connect("https://vieon.vn/")
                        .cookies(cookie)
                        .execute()
                        .cookies())
                }

                Jsoup.connect(tvChannel.tvChannelWebDetailPage)
                    .cookies(cookie)
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("accept-language", "en-US,en;q=0.9,vi;q=0.8")
                    .header("cache-control", "no-cache")
                    .header("pragma", "no-cache")
                    .header(
                        "user-agent",
                        "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36"
                    )
                    .header("referer", tvChannel.tvChannelWebDetailPage)
                    .header("origin", tvChannel.tvChannelWebDetailPage.getBaseUrl())
                    .execute()
                    .also {
                        cookie.putAll(it.cookies())
                    }
                    .parse()
                    .body()
            } catch (e: java.lang.Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
                return@create
            }
            if (emitter.isDisposed) {
                return@create
            }
            val timeStamp = Calendar.getInstance().time
            val timeStampExp = Calendar.getInstance().apply {
                this.timeInMillis += 48 * 24 * 60 * 60 * 1000L
            }.time
            val randomId: String = UUID.randomUUID().toString()
            val jti: String = UUID.randomUUID().toString().replace("-", "")
            body.getElementById("__NEXT_DATA__")?.let {
                val text = it.html()
                val jsonObject = JSONObject(text)
                val token = jsonObject.optJSONObject("props")
                    ?.optJSONObject("initialState")
                    ?.optJSONObject("App")
                    ?.optString("token")
                    ?.also {
                        Logger.d(this@VDataSourceImpl, message = "token: $it")
                    }
                val listStreamLink = try {
                    getTVLinkStream(
                        token = buildJWT(
                            timeStamp = timeStamp,
                            timeStampExp = timeStampExp,
                            randomId = randomId,
                            jti = jti,
                        ),
                        tvChannel = tvChannel,
                        slug = tvChannel.tvChannelWebDetailPage.removePrefix("https://vieon.vn"),
                        profileToken = buildProfileToken(
                            timeStamp = timeStamp,
                            timeStampExp = timeStampExp,
                            randomId = randomId,
                            jti = jti
                        )
                    )
                } catch (e: Exception) {
                    Logger.e(this@VDataSourceImpl, exception = e)
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                    return@create
                }
                if (emitter.isDisposed) {
                    return@create
                }
                if (listStreamLink.isEmpty()) {
                    Log.d("TAG", "Empty Data")
                    emitter.onError(Throwable("Empty Data"))
                } else {
                    emitter.onNext(
                        TVChannelLinkStream(
                            tvChannel,
                            listStreamLink.map {
                                TVChannel.Url.fromUrl(
                                    url = it,
                                    referer = tvChannel.tvChannelWebDetailPage,
                                    origin = tvChannel.tvChannelWebDetailPage.getBaseUrl()
                                )
                            }
                        )
                    )
                }
            }
            if (emitter.isDisposed) {
                return@create
            }
            emitter.onComplete()
        }
    }

    private fun fetchTvList(
        name: String,
        onComplete: (list: List<TVChannel>) -> Unit
    ): Task<DataSnapshot> {
        return firebaseDatabase.reference.child(name)
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
                            channelId = if (name in listOf(
                                    TVChannelGroup.VOV.name,
                                    TVChannelGroup.VOH.name
                                )
                            ) {
                                dataFromFirebase.name.removeAllSpecialChars()
                            } else {
                                dataFromFirebase.url.trim().removeSuffix("/").split("/").last()
                            }
                        )
                    })
            }
    }

    private fun getTVLinkStream(
        token: String,
        tvChannel: TVChannel,
        slug: String,
        profileToken: String
    ): MutableList<String> {
        val baseUrl = apiUrl
        val request = Request.Builder()
            .url(baseUrl)
            .header("Accept", "application/json, text/plain, */*")
            .header(
                "Accept-Language",
                "vi-VN,vi;q=0.9,fr-FR;q=0.8,fr;q=0.7,en-US;q=0.6,en;q=0.5,am;q=0.4,en-AU;q=0.3"
            )
            .header("Authorization", token)
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json;charset=UTF-8")
            .header("Origin", "https://vieon.vn")
            .header("Referer", tvChannel.tvChannelWebDetailPage)
            .header("Profile-Token", profileToken)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36"
            )
            .post(
                FormBody.Builder()
                    .addEncoded(
                        "livetv_slug",
                        slug.trim().takeIf { it.isNotEmpty() }
                            ?: tvChannel.tvChannelWebDetailPage.removePrefix("https://vieon.vn")
                    )
                    .addEncoded("platform", "web")
                    .addEncoded("ui", "012021")
                    .build()
            )
        val response = httpClient.newCall(request.build())
            .execute()
            .body.string()
        val listStreamLink = mutableListOf<String>()
        Logger.d(this@VDataSourceImpl, message = "response: $response")
        val json = JSONObject(response)
        json.optString("hls_link_play").trim().takeIf {
            it.isNotEmpty()
        }?.let {
            listStreamLink.add(it)
        }
        json.optString("dash_link_play").trim().takeIf {
            it.isNotEmpty()
        }?.let {
            listStreamLink.add(it)
        }
        json.optString("link_play").trim().takeIf {
            it.isNotEmpty()
        }?.let {
            listStreamLink.add(it)
        }
        json.optJSONObject("play_links")
            ?.optJSONObject("h264")
            ?.let {
                it.optString("hls").trim().takeIf {
                    it.isNotEmpty()
                }?.let {
                    listStreamLink.add(it)
                }
                it.optString("dash").trim().takeIf {
                    it.isNotEmpty()
                }?.let {
                    listStreamLink.add(it)
                }
            }

        json.optJSONObject("play_links")
            ?.optJSONObject("h265")
            ?.let {
                it.optString("hls").trim().takeIf {
                    it.isNotEmpty()
                }?.let {
                    listStreamLink.add(it)
                }
                it.optString("dash").trim().takeIf {
                    it.isNotEmpty()
                }?.let {
                    listStreamLink.add(it)
                }
            }
        return listStreamLink
    }

    /// time_stamp_exp = time_stamp + 48 * HOUR
    /// time_stamp = System.currentTimeMillis() / 1000
    private val jwtDefault by lazy {
        "{\n" +
                "  \"iss\": \"VO\",\n" +
                "  \"sub\": \"$RANDOM_ID\",\n" +
                "  \"exp\": {time_stamp_exp},\n" +
                "  \"iat\": {time_stamp},\n" +
                "  \"jti\": \"$JTI\",\n" +
                "  \"scope\": \"$DEFAULT_SCOPE\",\n" +
                "  \"di\": \"$RANDOM_ID_2\",\n" +
                "  \"ua\": \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36\",\n" +
                "  \"dt\": \"web\",\n" +
                "  \"mth\": \"mobile\",\n" +
                "  \"ispre\": 0,\n" +
                "  \"md\": \"Mac OS 10.15.7\",\n" +
                "  \"version\": \"1626171878\",\n" +
                "  \"isg\": 0,\n" +
                "  \"adid\": \"\",\n" +
                "  \"aud\": \"VO\"\n" +
                "}"
    }

    private val profile by lazy {
        "{" +
                "\"exp\":{time_stamp_exp}," +
                "\"jti\": \"$JTI\"," +
                "\"iss\": \"vieon.vn\"," +
                "\"sub\": \"$RANDOM_ID\"," +
                "\"uid\": \"$RANDOM_ID\"," +
                "\"isKid\": 0\n" +
                "}"
    }

    private fun buildJWT(
        timeStamp: Date = Calendar.getInstance().time,
        timeStampExp: Date = Calendar.getInstance().apply {
            this.timeInMillis += 48 * 60 * 60 * 1000L
        }.time,
        randomId: String = UUID.randomUUID().toString(),
        randomId2: String = UUID.randomUUID().toString(),
        jti: String = UUID.randomUUID().toString(),
        scope: String = DEFAULT_SCOPE
    ): String {
        val key = Jwts.SIG.HS256.key().build()
        val jws = Jwts.builder()
            .issuer("VO")
            .subject(randomId)
            .expiration(timeStampExp)
            .issuedAt(timeStamp)
            .id(jti.replace("-", ""))
            .claim("scope", scope)
            .claim("di", randomId2.replace("-", ""))
            .claim("ua", Constants.USER_AGENT)
            .claim("dt", "web")
            .claim("mth", "mobile")
            .claim("md", "Mac OS 10.15.7")
            .claim("ispre", 0)
            .claim("version", "1626171878")
            .claim("isg", 0)
            .claim("adid", "")
            .setAudience("VO")
            .header()
            .add("typ", "JWT")
            .and()
            .signWith(key)
            .compact()
        return jws
    }

    private fun buildProfileToken(
        timeStamp: Date = Calendar.getInstance().time,
        timeStampExp: Date = Calendar.getInstance().apply {
            this.timeInMillis += 48 * 24 * 60 * 60 * 1000L
        }.time,
        randomId: String = UUID.randomUUID().toString(),
        jti: String = UUID.randomUUID().toString(),
    ): String {
        val profile = profile
            .replace("{time_stamp}", timeStamp.toString())
            .replace("{time_stamp_exp}", timeStampExp.toString())
            .replace(RANDOM_ID, randomId)
            .replace(JTI, jti)
            .also {
                Logger.d(this@VDataSourceImpl, message = "profileStr: $it")
            }
        val key = Jwts.SIG.HS256.key().build()
        val profileToken = Jwts.builder()
            .expiration(timeStampExp)
            .id(jti)
            .issuer("vieon.vn")
            .subject(randomId)
            .claim("uid", jti)
            .claim("isKid", 0)
            .header()
            .add("typ", "JWT")
            .and()
            .signWith(key)
            .compact()
        return profileToken
    }

    companion object {
        private const val HOUR = 60 * 60
        private const val RANDOM_ID = "{random_id}" // 20a0d9d2ebff609035fc1da808d99a64
        private const val RANDOM_ID_2 = "{random_id_2}" // 7d5b16a43d3a24dc379e47d2dd57bfe2
        private const val JTI = "{jti}" //6965615484c9a71c91a04a7f59c21c2a
        private const val DEFAULT_SCOPE = "cm:read cas:read cas:write billing:read"
    }

}
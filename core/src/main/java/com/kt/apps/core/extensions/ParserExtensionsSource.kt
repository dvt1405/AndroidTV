package com.kt.apps.core.extensions

import com.google.gson.Gson
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.NullPointerException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

@CoreScope
class ParserExtensionsSource @Inject constructor(
    private val client: OkHttpClient,
    private val storage: IKeyValueStorage,
) {

    fun parseFromRemoteRx(extension: ExtensionsConfig): Observable<List<ExtensionsChannel>> {
        return Observable.create<List<ExtensionsChannel>> {
            try {
                it.onNext(parseFromRemote(extension))
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .doOnNext {
                Logger.d(this@ParserExtensionsSource, message = Gson().toJson(it))
            }

    }

    fun parseFromRemote(extension: ExtensionsConfig): List<ExtensionsChannel> {
        trustEveryone()
        val response = client
            .newBuilder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
            .newCall(
                Request.Builder()
                    .url(extension.sourceUrl)
                    .build()
            ).execute()

        if (response.code in 200..299) {
            val bodyStr = response.body.string()
            if (!bodyStr.trim().startsWith(TAG_START)) {
                throw IllegalStateException(
                    "Not support for extension ${extension.sourceUrl} cause: " +
                            "Source must start with $TAG_START"
                )
            }
            val index = bodyStr.indexOf(TAG_EXT_INFO)

            return parseFromText(bodyStr.substring(index, bodyStr.length), extension)
        }
        return emptyList()
    }

    private fun parseFromText(text: String, extension: ExtensionsConfig): List<ExtensionsChannel> {
        return text.split(REGEX_MEDIA_DURATION)
            .map {
                it.trim().removePrefix(",")
            }.mapNotNull { itemStr ->
                extractChannel(itemStr, extension)
            }
    }

    private fun getKeyValueByRegex(regex: Pattern, finder: String): Pair<String, String> {
        val key = getByRegex(regex, finder)
        val startIndex = finder.indexOf("=")
        val value = finder.substring(startIndex + 1, finder.length)
            .trim()
            .removePrefix("\"")
            .removeSuffix("\r")
            .removeSuffix("\"")
        val realHttpKey = realKeys[key] ?: key
        return Pair(realHttpKey, value)
    }

    private fun extractChannel(
        itemStr: String,
        extension: ExtensionsConfig
    ): ExtensionsChannel? {
        var channelId = ""
        var channelLogo = ""
        var channelGroup = ""
        var channelName = ""
        var tvCatchupSource = ""
        var userAgent = ""
        var referer = ""
        var channelLink = ""
        val props = mutableMapOf<String, String>()

        itemStr.split("\n", "#")
            .filter {
                it.isNotBlank()
            }
            .forEach { line ->
                if (line.removePrefix("#").startsWith("http")) {
                    channelLink = line.trim().removePrefix("#").trim()
                        .replace("\t", "")
                        .replace("\b", "")
                        .replace("\r", "")
                        .replace(" ", "")
                        .replace("  ", "")
                        .trim()
                    while (channelLink.contains(TAG_REFERER)) {
                        val refererInChannelLink = getByRegex(REFERER_REGEX, line)
                        channelLink = channelLink.replace("$TAG_REFERER=$refererInChannelLink", "")
                            .trim()
                    }
                    channelLink = channelLink.trim()
                        .removeSuffix("#")
                        .trim()
                    Logger.d(this@ParserExtensionsSource, "ChannelLink", channelLink)
                }

                if (line.contains(CATCHUP_SOURCE_PREFIX)) {
                    tvCatchupSource = getByRegex(CHANNEL_CATCH_UP_SOURCE_REGEX, line)
                }

                if (line.contains(TAG_USER_AGENT)) {
                    userAgent = getByRegex(REGEX_USER_AGENT, line)
                }

                if (line.contains(TAG_REFERER)) {
                    referer = getByRegex(REFERER_REGEX, line)
                }

                when {
                    line.contains(LOGO_PREFIX) || line.contains(ID_PREFIX) || line.contains(TITLE_PREFIX) -> {
                        if (line.contains(ID_PREFIX)) {
                            channelId = getByRegex(CHANNEL_ID_REGEX, line)
                        }

                        if (line.contains(LOGO_PREFIX)) {
                            channelLogo = getByRegex(CHANNEL_LOGO_REGEX, line)
                        }

                        if (line.contains(TITLE_PREFIX)) {
                            channelGroup = getByRegex(CHANNEL_GROUP_TITLE_REGEX, line)
                        }

                        if (line.contains(",")) {
                            channelName = line.split(",").lastOrNull()
                                ?: ""
                        }

                    }

                    line.contains(TAG_EXTVLCOPT) -> {
                        val keyValue = getKeyValueByRegex(REGEX_EXTVLCOPT_PROP_KEY, line)
                        props[keyValue.first] = keyValue.second
                    }

                    line.contains(TAG_KODIPROP) -> {
                        val keyValue = getKeyValueByRegex(REGEX_KODI_PROP_KEY, line)
                        props[keyValue.first] = keyValue.second
                    }
                }
            }
        return try {
            if (channelLink.isBlank() && tvCatchupSource.isBlank()) {
                throw NullPointerException()
            }
            val channel = ExtensionsChannel(
                tvGroup = channelGroup,
                logoChannel = channelLogo,
                channelId = channelId,
                tvChannelName = channelName.trim().removeSuffix(" "),
                sourceFrom = extension.sourceName,
                tvStreamLink = channelLink.trim().removeSuffix(" "),
                isHls = channelLink.contains("m3u8"),
                catchupSource = tvCatchupSource.replace("\${start}", "${System.currentTimeMillis()}")
                    .replace("\${offset}", "${System.currentTimeMillis() + OFFSET_TIME}"),
                referer = referer,
                userAgent = userAgent,
                props = props
            )
            Logger.d(this@ParserExtensionsSource, "Channel", message = "$channel")
            channel
        } catch (e: Exception) {
            Logger.e(this, message = "Parser error: $itemStr")
            Logger.e(this, exception = e)
            null
        }
    }

    private fun getByRegex(pattern: Pattern, finder: String): String {
        val matcher = pattern.matcher(finder)
        while (matcher.find()) {
            return matcher.group(0) ?: ""
        }
        return ""
    }


    private fun getByRegex(regex: String, finder: String): String {
        val pt = Pattern.compile(regex)
        return getByRegex(pt, finder)
    }

    companion object {
        private const val OFFSET_TIME = 2 * 60 * 60 * 1000
        private const val EXTRA_EXTENSIONS_KEY = "extra:extensions_key"
        private const val TAG_START = "#EXTM3U"
        private const val TAG_EXT_INFO = "#EXTINF:"
        private const val TAG_REFERER = "|Referer"
        private val REGEX_MEDIA_DURATION = Pattern.compile("#EXTINF:( )?-?\\d+")
        private val REGEX_MEDIA_DURATION_2 = Pattern.compile("EXTINF:( )?-?\\d+")
        private const val URL_TVG_PREFIX = "url-tvg"
        private const val CACHE_PREFIX = "cache"
        private const val RATIO_PREFIX = "aspect-ratio"
        private const val DEINTERLACE_PREFIX = "deinterlace"
        private const val TVG_SHIFT_PREFIX = "tvg-shift"
        private const val M3U_AUTO_LOAD_PREFIX = "m3uautoload"
        private const val CATCHUP_SOURCE_PREFIX = "catchup-source"
        private const val TAG_USER_AGENT = "user-agent"
        private const val TAG_KODIPROP = "KODIPROP"
        private const val TAG_EXTVLCOPT = "EXTVLCOPT"
        private val REGEX_USER_AGENT = Pattern.compile("(?<=user-agent=\").*?(?=\")")

        private const val ID_PREFIX = "tvg-id"
        private const val LOGO_PREFIX = "tvg-logo"
        private const val TITLE_PREFIX = "group-title"
        private const val TYPE_PREFIX = "type"

        private val URL_TVG_REGEX = Pattern.compile("(?<=url-tvg=\").*?(?=\")")
        private val CACHE_REGEX = Pattern.compile("(?<=cache=).*?(?= )")
        private val DEINTERLACE_REGEX = Pattern.compile("(?<=deinterlace=).*?(?= )")
        private val RATIO_REGEX = Pattern.compile("(?<=aspect-ratio=).*?(?= )")
        private val TVG_SHIFT_REGEX = Pattern.compile("(?<=tvg-shift=).*?(?= )")
        private val M3U_AUTO_REGEX = Pattern.compile("(?<=m3uautoload=).*?(?= )")
        private val CHANNEL_ID_REGEX = Pattern.compile("(?<=tvg-id=\").*?(?=\")")
        private val CHANNEL_LOGO_REGEX = Pattern.compile("(?<=tvg-logo=\").*?(?=\")")
        private val CHANNEL_GROUP_TITLE_REGEX = Pattern.compile("(?<=group-title=\").*?(?=\")")
        private val CHANNEL_CATCH_UP_SOURCE_REGEX = Pattern.compile("(?<=catchup-source=\").*?(?=\")")
        private val REFERER_REGEX = Pattern.compile("(?<=\\|Referer=).*")
        private val CHANNEL_TYPE_REGEX = Pattern.compile("(?<=type=\").*?(?=\")")
        private val CHANNEL_TITLE_REGEX = Pattern.compile("(?<=\").*?(?=\")")
        private val REGEX_KODI_PROP_KEY = Pattern.compile("(?<=KODIPROP:).*?(?==)")
        private val REGEX_EXTVLCOPT_PROP_KEY = Pattern.compile("(?<=EXTVLCOPT:).*?(?==)")
        private val realKeys = mapOf(
            "http-referrer" to "referer",
            "http-user-agent" to "user-agent"
        )
    }

}
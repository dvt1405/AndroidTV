package com.kt.apps.core.usecase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import io.reactivex.rxjava3.core.Observable
import org.json.JSONArray
import javax.inject.Inject

class GetCurrentProgrammeForChannel @Inject constructor(
    private val parser: ParserExtensionsProgramSchedule,
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : BaseUseCase<TVScheduler.Programme>() {
    private val mappingEpgId by lazy {
        mutableMapOf<String, String>()
    }
    private fun getMappingEpgChannelId(): Map<String, String> {
        try {
            if (mappingEpgId.isNotEmpty()) return mappingEpgId
            val remoteMapping = firebaseRemoteConfig.getString("tv_epg_mapping")
            val jsonArr = JSONArray(remoteMapping)
            Logger.d(this@GetCurrentProgrammeForChannel, message = "{\"RemoteMapping\": $remoteMapping}")
            for (i in 0 until jsonArr.length()) {
                val key = jsonArr.optJSONObject(i)?.optString("key") ?: continue
                val value = jsonArr.optJSONObject(i)?.optString("value") ?: continue
                mappingEpgId[key] = value
            }
            if (mappingEpgId.isNotEmpty()) {
                return mappingEpgId
            }
        } catch (_: Exception) {
        }
        return mapping
    }

    override fun prepareExecute(params: Map<String, Any>): Observable<TVScheduler.Programme> {
        return when (val channel = params[EXTRA_CHANNEL]) {
            is String -> {
                val defaultSource = parser.getCurrentProgramForTVChannel(channel)
                val extraSource = getMappingEpgChannelId()[channel]?.split("|")?.map { newId ->
                    parser.getCurrentProgramForTVChannel(newId)
                        .map {
                            Logger.d(this@GetCurrentProgrammeForChannel, message = "$it")
                            TVScheduler.Programme(
                                channel = channel,
                                channelNumber = it.channelNumber,
                                start = it.start,
                                stop = it.stop,
                                title = it.title,
                                description = it.description,
                                extensionsConfigId = it.extensionsConfigId,
                                extensionEpgUrl = it.extensionEpgUrl
                            )
                        }
                }?.reduce { acc, observable ->
                    acc.mergeWith(observable)
                }
                (extraSource?.concatWith(defaultSource) ?: defaultSource).switchIfEmpty {
                    it.onError(Throwable("Empty program for channel id: $channel"))
                }
            }

            is ExtensionsChannel -> {
                parser.getCurrentProgramForExtensionChannel(
                    channel, params[EXTRA_CHANNEL_TYPE] as? ExtensionsConfig.Type
                        ?: ExtensionsConfig.Type.MOVIE
                )
            }

            else -> {
                Observable.error<TVScheduler.Programme>(Throwable("Null params not supported"))
            }
        }
    }

    operator fun invoke(tvChannelId: String): Observable<TVScheduler.Programme> {
        return execute(
            mapOf(
                EXTRA_CHANNEL to tvChannelId
            )
        )
    }

    operator fun invoke(
        extensionsChannel: ExtensionsChannel,
        configType: ExtensionsConfig.Type
    ): Observable<TVScheduler.Programme> {
        return execute(
            mapOf(
                EXTRA_CHANNEL to extensionsChannel,
                EXTRA_CHANNEL_TYPE to configType
            )
        )
    }

    companion object {
        private const val EXTRA_CHANNEL = "extra:channel"
        private const val EXTRA_CHANNEL_TYPE = "extra:channel_type"
        val mapping by lazy {
            mapOf(
                "thvl1-hd" to "vinhlong1hd",
                "thvl2-hd" to "vinhlong2hd",
                "thvl3-hd" to "vinhlong3hd",
                "thvl4-hd" to "vinhlong4hd",
                "vtv-can-tho" to "vtv6",
                "vtc2-reidius-tv" to "vtc2",
                "vtc16-hd" to "vtc16",
                "vtc4-yeah1-family-hd" to "vtc4",
                "vtc7-todaytv-hd" to "vtc7|todaytv",
                "vtc10-1" to "vtc10",
                "vtc11-kids-tv" to "vtc11",
                "htvc-du-lich-cuoc-song" to "htvcdulichhd",
                "htvc-du-lich-cuoc-song" to "htvcdulichhd",
                "an-giang-1" to "angiang",
                "bac-giang-hd" to "bacgiang",
                "bac-kan-1" to "backan",
                "bac-ninh-1" to "bacninh",
                "ben-tre-1-2" to "bentre",
                "binh-dinh" to "binhdinh",
                "binh-duong-1" to "binhduong1",
                "binh-duong-2" to "binhduong2",
                "binh-duong-4" to "binhduong4",
                "binh-phuoc-1" to "binhphuoc1",
                "binh-phuoc-2" to "binhphuoc2",
                "binh-thuan-1" to "binh-thuan",
                "ca-mau-1" to "camau",
                "can-tho-1" to "cantho",
                "dien-bien-20" to "dienbien",
                "dong-thap-1" to "dongthap",
                "gia-lai-1" to "gialai",
                "ha-giang-1" to "hagiang",
                "ha-nam-1" to "hanam",
                "ha-noi-2-2021" to "hanoi2",
                "ha-tinh-1" to "hatinh",
                "hai-phong-1" to "haiphong",
                "hau-giang-1" to "haugiang",
                "hoa-binh-1" to "hoabinh",
                "khanh-hoa-1" to "khanhhoa",
                "kon-tum-1" to "kontum",
                "lang-son-1" to "langson",
                "long-an-1" to "longan",
                "nghe-an-1" to "nghean",
                "ninh-binh-11" to "ninhbinh",
                "ninh-thuan-1" to "ninhthuan",
                "quang-binh-1" to "quangbinh",
                "quang-nam-1" to "quangnam",
                "quang-ngai-1" to "quangngai",
                "quang-ninh-1" to "quangninh1",
                "quang-ninh-3" to "quangninh3",
                "quang-tri-1" to "quangtri",
                "soc-trang-1" to "soctrang",
                "tay-ninh-3" to "tayninh",
                "thai-binh-1" to "thaibinh",
                "thai-nguyen-1" to "thainguyen",
                "thanh-hoa-48" to "thanhhoa",
                "thua-thien-hue-1" to "hue",
                "tien-giang-1" to "tiengiang",
                "tra-vinh-1" to "travinh",
                "tuyen-quang-1" to "tuyenquang",
                "vinh-phuc-1" to "vinhphuc",
                "ba-ria-vung-tau-1" to "vungtau",
                "yen-bai-1" to "yenbai",
                "nhan-dan-hd" to "nhandan",
                "quoc-hoi-viet-nam-hd" to "quochoi",
                "vnews-hd" to "ttxvnhd",
                "quoc-phong-hd" to "qpvnhd",
                "an-ninh-hd" to "antvhd|antv"
            )
        }
    }


}
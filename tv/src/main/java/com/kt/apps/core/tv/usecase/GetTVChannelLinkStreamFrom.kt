package com.kt.apps.core.tv.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.tv.FirebaseLogUtils
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetTVChannelLinkStreamFrom @Inject constructor(
    private val mapDataSource: Map<TVDataSourceFrom, @JvmSuppressWildcards ITVDataSource>,
) : BaseUseCase<TVChannelLinkStream>() {

    private val mapSourceBackup by lazy {
        mapOf(
            TVChannelGroup.VTC.name to TVDataSourceFrom.VTC_BACKUP,
            TVChannelGroup.VTV.name to TVDataSourceFrom.VTV_BACKUP,
            TVChannelGroup.HTV.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.HTVC.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.Intenational.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.AnNinh.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.THVL.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.DiaPhuong.name to TVDataSourceFrom.HTV_BACKUP,
            TVChannelGroup.VOV.name to TVDataSourceFrom.VOV_BACKUP,
            TVChannelGroup.VOH.name to TVDataSourceFrom.VOH_BACKUP,
//            TVChannelGroup.Others.name to TVDataSourceFrom.HTV_BACKUP
        )
    }

    override fun prepareExecute(params: Map<String, Any>): Observable<TVChannelLinkStream> {
        val tvDetail = params[EXTRA_TV_CHANNEL] as TVChannel
        val sourceFrom = params[EXTRA_SOURCE_FROM] as TVDataSourceFrom


        if (sourceFrom == TVDataSourceFrom.V && tvDetail.tvGroup == TVChannelGroup.VOV.name) {
            return Observable.just(TVChannelLinkStream(tvDetail, listOf(tvDetail.tvChannelWebDetailPage).map {
                TVChannel.Url.fromUrl(it)
            }))
        }

        if (sourceFrom == TVDataSourceFrom.V && tvDetail.tvGroup == TVChannelGroup.VOH.name) {
            return Observable.just(TVChannelLinkStream(tvDetail, listOf(tvDetail.tvChannelWebDetailPage).map {
                TVChannel.Url.fromUrl(it)
            }))
        }
//        if (sourceFrom == TVDataSourceFrom.V && tvDetail.tvGroup == TVChannelGroup.Others.name) {
//            return Observable.just(TVChannelLinkStream(tvDetail, listOf(tvDetail.tvChannelWebDetailPage)))
//        }
        if (sourceFrom == TVDataSourceFrom.V && tvDetail.tvChannelWebDetailPage.contains(";stream")) {
            return Observable.just(TVChannelLinkStream(tvDetail, listOf(tvDetail.tvChannelWebDetailPage).map {
                TVChannel.Url.fromUrl(it)
            }))
        }

        if (sourceFrom == TVDataSourceFrom.V) {
            return mapDataSource[sourceFrom]!!.getTvLinkFromDetail(tvDetail)
                .onErrorResumeNext {
                    FirebaseLogUtils.logGetLinkVideoM3u8Error(
                        tvDetail,
                        (it.message ?: it::class.java.name)
                    )
                    val backupSource = mapSourceBackup[tvDetail.tvGroup]
                    tvDetail.sourceFrom = backupSource?.name ?: tvDetail.sourceFrom
                    backupSource?.let {
                        mapDataSource[it]!!.getTvLinkFromDetail(tvDetail, true)
                    } ?: Observable.error(it)
                }
        }

        return mapDataSource[sourceFrom]!!.getTvLinkFromDetail(
            tvDetail,
            params[EXTRA_IS_BACKUP] as? Boolean ?: false
        )
            .doOnNext {
                FirebaseLogUtils.logGetLinkVideoM3u8(tvDetail)
            }.doOnError {
                FirebaseLogUtils.logGetLinkVideoM3u8Error(
                    tvDetail,
                    (it.message ?: it::class.java.name)
                )
            }

    }

    operator fun invoke(tvChannel: TVChannel) = execute(
        mapOf(
            EXTRA_TV_CHANNEL to tvChannel,
            EXTRA_SOURCE_FROM to TVDataSourceFrom.valueOf(tvChannel.sourceFrom)
        )
    )

    operator fun invoke(tvChannel: TVChannel, isBackup: Boolean): Observable<TVChannelLinkStream> =
        execute(
            mapOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_SOURCE_FROM to TVDataSourceFrom.valueOf(tvChannel.sourceFrom),
                EXTRA_IS_BACKUP to isBackup
            )
        )

    companion object {
        private const val EXTRA_SOURCE_FROM = "extra:datasource_from"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        private const val EXTRA_IS_BACKUP = "extra:is_backup"
    }
}
package com.kt.apps.media.xemtv.workers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import androidx.tvprovider.media.tv.*
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.TVChannelEntity
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.workers.factory.ChildWorkerFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class TVRecommendationWorkers(
    private val context: Context,
    private val params: WorkerParameters
) : Worker(context, params) {

    private val disposable by lazy {
        CompositeDisposable()
    }
    private val tvChannelDAO by lazy {
        RoomDataBase.getInstance(context)
            .tvChannelRecommendationDao()
    }

    override fun doWork(): Result {
        return try {
            when (params.inputData.getInt(EXTRA_TYPE, Type.ALL.value)) {
                Type.WATCH_NEXT.value -> {
                    insertOrUpdateWatchNextChannel(
                        params.inputData.getString(EXTRA_TV_PROGRAM_ID)!!
                    )
                }

                else -> {
                    insertOrUpdatePreviewChannel()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    fun resourceUri(resources: Resources, id: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()

    @SuppressLint("RestrictedApi")
    fun insertOrUpdateWatchNextChannel(programId: String) {
        val watchNextTask = tvChannelDAO.getChannelByID(programId)
            .flatMapCompletable { tvChannelEntity ->
                val allWatchNext = getWatchNextPrograms(context)
                allWatchNext.forEach {
                    Logger.d(this@TVRecommendationWorkers, "WatchNextChannel", "$it")
                }
                allWatchNext.filter {
                    it.contentId != programId
                }.forEach {
                    val programUri = TvContractCompat.buildWatchNextProgramUri(it.id)
                    context.contentResolver.delete(
                        programUri, null, null
                    )
                }
                val existWatchNextProgram = allWatchNext.find {
                    it.contentId == programId
                }
                Logger.d(this@TVRecommendationWorkers, "WatchNextChannel", "$existWatchNextProgram")
                val builder = existWatchNextProgram?.let {
                    WatchNextProgram.Builder(it)
                } ?: WatchNextProgram.Builder()

                val continueProgram = builder.setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                    .setIntentUri(Uri.parse("xemtv://tv/channel/${tvChannelEntity.channelId}"))
                    .setContentId(tvChannelEntity.channelId)
                    .setBrowsable(true)
                    .setTitle(tvChannelEntity.tvChannelName)
                    .setThumbnailUri(tvChannelEntity.logoChannel)
                    .setSearchable(true)
                    .setPosterArtUri(tvChannelEntity.logoChannel)
                    .setLogoUri(tvChannelEntity.logoChannel)
                    .setReleaseDate(Calendar.getInstance(Locale.TAIWAN).time)
                    .setType(TvContractCompat.WatchNextPrograms.TYPE_CHANNEL)
                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                    .build()
                Logger.d(this@TVRecommendationWorkers, "WatchNextChannel", "$continueProgram")

                val id = existWatchNextProgram?.let {
                    PreviewChannelHelper(context)
                        .updateWatchNextProgram(continueProgram, it.id)
                    it.id
                } ?: let {
                    PreviewChannelHelper(context)
                        .publishWatchNextProgram(continueProgram)
                }
                Logger.d(this@TVRecommendationWorkers, "WatchNextChannel", "Insert id: $id")
                Completable.complete()
            }
            .subscribe({
                Logger.d(this@TVRecommendationWorkers, message = "Update success")
            }, {
                Logger.e(this@TVRecommendationWorkers, exception = it)
            })

        disposable.add(watchNextTask)
    }

    @SuppressLint("RestrictedApi")
    @Synchronized
    fun insertOrUpdatePreviewChannel() {
        val tvChannelUseCase = (context.applicationContext as App)
            .tvComponents
            .getListTVChannel()

        disposable.add(
            tvChannelUseCase.invoke(false)
                .observeOn(Schedulers.computation())
                .flatMap {
                    io.reactivex.rxjava3.core.Observable.just(
                        it.filter { !it.isRadio },
                        it.filter { it.isRadio },
                    )
                }
                .filter {
                    it.isNotEmpty()
                }
                .flatMapCompletable { channelList ->
                    Logger.d(this@TVRecommendationWorkers, message = "Size: ${channelList.size}")
                    val isRadio = channelList.first().isRadio
                    val allChannels: List<PreviewChannel> = try {
                        getAllChannels(context)
                    } catch (exc: IllegalArgumentException) {
                        listOf()
                    }
                    if (DEBUG) {
                        allChannels.forEach {
                            Logger.d(this, message = "$it")
                        }
                    }

                    val tvChannelProviderId: String = if (isRadio) {
                        "radioChannelIMedia"
                    } else {
                        "tvChannelIMedia"
                    }

                    val displayName: String = if (isRadio) {
                        RADIO_PREVIEW_CHANNEL_DISPLAY_NAME
                    } else {
                        TV_PREVIEW_CHANNEL_DISPLAY_NAME
                    }

                    val channelUri = if (isRadio) {
                        Uri.parse("xemtv://radio/dashboard")
                    } else {
                        Uri.parse("xemtv://tv/dashboard")
                    }

                    val existingChannel = allChannels.find { it.internalProviderId == tvChannelProviderId }

                    if (DEBUG) {
                        Logger.d(this, message = "existingChannel: $existingChannel")
                    }

                    val channelBuilder = if (existingChannel == null) {
                        PreviewChannel.Builder()
                    } else {
                        PreviewChannel.Builder(existingChannel)
                    }

                    Logger.e(
                        this@TVRecommendationWorkers,
                        message = resourceUri(App.get().resources, com.kt.apps.core.R.drawable.app_icon_fg).toString()
                    )
                    val id = com.kt.apps.core.R.drawable.app_icon_fg
                    val logoUri = if (id == -1) {
                        Uri.parse("android.resource://com.kt.apps.media.xemtv/drawable/app_icon_fg")
                    } else {
                        resourceUri(App.get().resources, com.kt.apps.core.R.drawable.app_icon_fg)
                    }
                    val channelUpdate = channelBuilder.setDisplayName(displayName)
                        .setLogo(Uri.parse("android.resource://${context.packageName}/drawable/app_icon_fg"))
                        .setDescription("iMedia")
                        .setInternalProviderId(tvChannelProviderId)
                        .setAppLinkIntentUri(channelUri)
                        .build()

                    val channelProviderId = if (existingChannel == null) {
                        PreviewChannelHelper(context)
                            .publishChannel(channelUpdate)
                    } else {
                        PreviewChannelHelper(context)
                            .updatePreviewChannel(
                                existingChannel.id,
                                channelUpdate
                            )
                        existingChannel.id
                    }

                    val channelEntity = channelList.map(mapToEntity(channelProviderId))

                    if (allChannels.none { it.isBrowsable }) {
                        TvContractCompat.requestChannelBrowsable(context, channelProviderId)
                    }

                    val existingProgramList = getPreviewPrograms(context, channelProviderId)

                    channelEntity.forEach { tvChannel ->

                        val existingProgram = existingProgramList.find { it.contentId == tvChannel.channelId }

                        val programBuilder = if (existingProgram == null) {
                            PreviewProgram.Builder()
                        } else {
                            PreviewProgram.Builder(existingProgram)
                        }

                        val updatedProgram = programBuilder.setContentId(tvChannel.channelId)
                            .setLogoUri(tvChannel.logoChannel)
                            .setTitle(tvChannel.tvChannelName)
                            .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                            .setChannelId(channelProviderId)
                            .setThumbnailUri(tvChannel.logoChannel)
                            .setBrowsable(true)
                            .setDurationMillis(0)
                            .setPosterArtUri(tvChannel.logoChannel)
                            .setReleaseDate(Calendar.getInstance(Locale.TAIWAN).time)
                            .setSearchable(true)
                            .setPosterArtUri(tvChannel.logoChannel)
                            .setIntentUri(Uri.parse("xemtv://tv/channel/${tvChannel.channelId}"))
                            .build()

                        try {
                            if (existingProgram == null) {
                                PreviewChannelHelper(context)
                                    .publishPreviewProgram(updatedProgram)
                            } else {
                                PreviewChannelHelper(context)
                                    .updatePreviewProgram(existingProgram.id, updatedProgram)
                            }
                        } catch (e: IllegalArgumentException) {
                            if (DEBUG) {
                                Logger.e(this@TVRecommendationWorkers, exception = e)
                            }
                        }

                    }
                    tvChannelDAO.insert(channelEntity)
                }
                .retry(3)
                .subscribe({
                    Logger.d(this@TVRecommendationWorkers, message = "Insert preview channel success")
                }, {
                    Logger.e(this@TVRecommendationWorkers, exception = it)
                })

        )
    }

    private fun mapToEntity(providerId: Long) = { channel: TVChannel ->
        val channelLogoName = if (channel.sourceFrom == TVDataSourceFrom.MAIN_SOURCE.name) {
            channel.logoChannel
        } else {
            Constants.mapChannel[channel.tvChannelName]!!
        }.removeSuffix(".png")
            .removeSuffix(".jpg")
            .removeSuffix(".webp")
            .removeSuffix(".jpeg")

        val logoUri = try {
            val id = context.resources.getIdentifier(
                channelLogoName,
                "drawable",
                context.packageName
            )

            if (id == 0 || id == -1) {
                throw Exception("Not found local resource for channel: $channel")
            }
            resourceUri(context.resources, id)
        } catch (e: Exception) {
            Uri.parse(channel.logoChannel)
        }

        TVChannelEntity(
            tvChannelName = channel.tvChannelName,
            channelId = channel.channelId,
            sourceFrom = channel.sourceFrom,
            logoChannel = logoUri,
            tvGroup = channel.tvGroup,
            tvChannelWebDetailPage = channel.tvChannelWebDetailPage,
            channelPreviewProviderId = providerId
        )
    }

    enum class Type(val value: Int) {
        ALL(1), WATCH_NEXT(2)
    }

    companion object {

        private const val DEBUG = false
        const val EXTRA_TYPE = "extra:type"
        const val EXTRA_TV_PROGRAM_ID = "extra:program_id"
        private const val TV_PREVIEW_CHANNEL_DISPLAY_NAME = "Truyền hình"
        private const val RADIO_PREVIEW_CHANNEL_DISPLAY_NAME = "Phát thanh"
        @SuppressLint("RestrictedApi")
        fun getAllChannels(context: Context): List<PreviewChannel> {
            val cursor: Cursor? = context.contentResolver
                .query(
                    TvContractCompat.Channels.CONTENT_URI,
                    PreviewChannel.Columns.PROJECTION,
                    null,
                    null,
                    null
                )
            val channels: MutableList<PreviewChannel> = ArrayList()
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        val channel = PreviewChannel.fromCursor(cursor)
                        channels.add(channel)
                    } catch (_: java.lang.Exception) {
                    }
                } while (cursor.moveToNext())
            }
            cursor?.close()
            return channels
        }

        @SuppressLint("RestrictedApi")
        fun getPreviewPrograms(context: Context, channelId: Long? = null): List<PreviewProgram> {
            val programs: MutableList<PreviewProgram> = mutableListOf()
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    PreviewProgram.PROJECTION,
                    null,
                    null,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val program = PreviewProgram.fromCursor(cursor)
                        if (channelId == null || channelId == program.channelId) {
                            programs.add(program)
                        }
                    } while (cursor.moveToNext())
                }

            } catch (exc: IllegalArgumentException) {
                Logger.e(this, "Error", exc)
            } finally {
                try {
                    cursor?.close()
                } catch (_: Exception) {
                }
            }

            return programs
        }

        @SuppressLint("RestrictedApi")
        fun getWatchNextPrograms(context: Context): List<WatchNextProgram> {
            val programs: MutableList<WatchNextProgram> = mutableListOf()

            try {
                val cursor = context.contentResolver.query(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    WatchNextProgram.PROJECTION,
                    null,
                    null,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        programs.add(WatchNextProgram.fromCursor(cursor))
                    } while (cursor.moveToNext())
                }
                cursor?.close()

            } catch (exc: IllegalArgumentException) {
                Logger.e(this, "Error", exc)
            }

            return programs
        }
    }

    class Factory @Inject constructor() : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker {
            return TVRecommendationWorkers(appContext, params)
        }
    }
}
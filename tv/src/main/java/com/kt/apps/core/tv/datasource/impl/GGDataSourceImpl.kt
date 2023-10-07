package com.kt.apps.core.tv.datasource.impl

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.*
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class GGDataSourceImpl @Inject constructor(
    private val compositeDisposable: DisposableContainer,
    private val keyValueStorage: TVStorage,
    private val roomDataBase: RoomDataBase,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseDatabase: FirebaseDatabase
) : ITVDataSource {

    override fun getTvList(): Observable<List<TVChannel>> {

        return Observable.create { emitter ->
            val listData = mutableListOf<TVChannel>()
            var totalRecordRead = 0
            supportTVChannelGroups.forEach { tvChannelGroup ->
                val db = if (tvChannelGroup == TVChannelGroup.VOH || tvChannelGroup == TVChannelGroup.VOV) {
                    firebaseDatabase.reference.child(tvChannelGroup.name)
                } else {
                    firebaseDatabase.getReference("GGSource")
                        .child(tvChannelGroup.name)
                }

                fetchTvList(db, tvChannelGroup.name) {
                    totalRecordRead++
                    listData.addAll(it)
                    emitter.onNext(it)
                    saveToRoomDB(tvChannelGroup, it)
                    Logger.d(this@GGDataSourceImpl, message = Gson().toJson(it))
                    if (totalRecordRead == supportTVChannelGroups.size) {
                        emitter.onComplete()
                    }
                }.addOnFailureListener {
                    Logger.e(this@GGDataSourceImpl, exception = it)
                    totalRecordRead++
                }
            }
        }
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        return Observable.just(
            TVChannelLinkStream(
                tvChannel,
                listOf(tvChannel.tvChannelWebDetailPage).map {
                    TVChannel.Url.fromUrl(it)
                }
            )
        )
    }

    private fun fetchTvList(
        db: DatabaseReference,
        recordName: String,
        onComplete: (list: List<TVChannel>) -> Unit
    ): Task<DataSnapshot> {
        return db
            .get()
            .addOnSuccessListener {
                val value = if (recordName == TVChannelGroup.VOV.name || recordName == TVChannelGroup.VOH.name) {
                    it.getValue<List<DataFromFirebase?>>() ?: return@addOnSuccessListener
                } else {
                    it.getValue<List<TVChannelFromFirebase?>>() ?: return@addOnSuccessListener
                }
                onComplete(
                    value.filterNotNull().map {
                        if (it is TVChannelFromFirebase) {
                            TVChannel(
                                it.tvGroup,
                                it.logoChannel,
                                it.tvChannelName,
                                it.tvChannelWebDetailPage,
                                if (recordName == TVChannelGroup.VOV.name || recordName == TVChannelGroup.VOH.name) {
                                    TVDataSourceFrom.V.name
                                } else {
                                    TVDataSourceFrom.GG.name
                                },
                                it.channelId
                            )
                        } else {
                            val channel = it as DataFromFirebase
                            TVChannel(
                                recordName,
                                tvChannelName = channel.name,
                                tvChannelWebDetailPage = channel.url,
                                logoChannel = channel.logo,
                                sourceFrom = TVDataSourceFrom.V.name,
                                channelId = channel.name.removeAllSpecialChars()
                            )
                        }

                    }
                )
            }
    }

    private fun saveToRoomDB(group: TVChannelGroup, tvDetails: List<TVChannel>) {
        compositeDisposable.add(
            roomDataBase.mapChannelDao()
                .insert(
                    tvDetails.map {
                        MapChannel(
                            channelId = it.channelId,
                            channelName = it.tvChannelName,
                            fromSource = it.sourceFrom,
                            channelGroup = group.name
                        )
                    }
                )
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                })
        )
    }

    data class TVChannelFromFirebase @JvmOverloads constructor(
        var tvGroup: String = "",
        var logoChannel: String = "",
        var tvChannelName: String = "",
        var tvChannelWebDetailPage: String = "",
        var sourceFrom: String = "",
        var channelId: String = ""
    )

    companion object {
        private val supportTVChannelGroups by lazy {
            listOf(
                TVChannelGroup.VTV,
                TVChannelGroup.HTV,
                TVChannelGroup.VTC,
                TVChannelGroup.HTVC,
                TVChannelGroup.THVL,
                TVChannelGroup.DiaPhuong,
                TVChannelGroup.Intenational,
                TVChannelGroup.SCTV,
                TVChannelGroup.Kid,
                TVChannelGroup.Radio,
                TVChannelGroup.VTVCAB,
                TVChannelGroup.Music,

                TVChannelGroup.VOV,
                TVChannelGroup.VOH,
            )
        }
    }
}
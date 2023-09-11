package com.kt.apps.core.tv.di

import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.impl.*
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVDataSourceFrom
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
open class TVChannelModule {



    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.V)
    @TVScope
    fun providesTVDataSource(vDataSourceImpl: VDataSourceImpl): ITVDataSource = vDataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.HY)
    @TVScope
    fun providesHYTVDataSource(hyDataSourceImpl: HYDataSourceImpl): ITVDataSource = hyDataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.VTV_BACKUP)
    @TVScope
    fun providesVTVBackupDataSource(vDataSourceImpl: VTVBackupDataSourceImpl): ITVDataSource = vDataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.VTC_BACKUP)
    @TVScope
    fun providesVTCBackupDataSource(vDataSourceImpl: VtcBackupDataSourceImpl): ITVDataSource = vDataSourceImpl


    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.VOV_BACKUP)
    @TVScope
    fun providesVOVBackupDataSource(vDataSourceImpl: VOVDataSourceImpl): ITVDataSource = vDataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.HTV_BACKUP)
    @TVScope
    fun providesHTVDataSource(vDataSourceImpl: HTVBackUpDataSourceImpl): ITVDataSource = vDataSourceImpl


    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.GG)
    @TVScope
    fun providesGGDataSource(dataSourceImpl: GGDataSourceImpl): ITVDataSource = dataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.SCTV)
    @TVScope
    fun providesSCTVDataSource(vDataSourceImpl: SCTVDataSourceImpl): ITVDataSource = vDataSourceImpl

    @Provides
    @IntoMap
    @TVDataSourceMapKey(TVDataSourceFrom.MAIN_SOURCE)
    @TVScope
    fun providesMainDataSourceDataSource(dataSourceImpl: MainTVDataSource): ITVDataSource = dataSourceImpl

    @Provides
    @TVScope
    open fun providesTimeout(): Long? = null

    @Provides
    @Singleton
    fun providesMapBackupDataSource(): Map<String, TVDataSourceFrom> = mapOf(
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
//        TVChannelGroup.Others.name to TVDataSourceFrom.HTV_BACKUP
    )

}
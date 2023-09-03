package com.kt.apps.core.tv.viewmodels

import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.tv.usecase.GetListTVChannel
import com.kt.apps.core.tv.usecase.GetTVChannelLinkStreamFrom
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import javax.inject.Inject

@TVScope
data class TVChannelInteractors @Inject constructor(
    val getListChannel: GetListTVChannel,
    val getChannelLinkStream: GetTVChannelLinkStreamFrom,
    val getChannelLinkStreamById: GetChannelLinkStreamById,
    val getCurrentProgrammeForChannel: GetCurrentProgrammeForChannel,
    val getListProgrammeForChannel: GetListProgrammeForChannel
)
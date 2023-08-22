package com.kt.apps.media.mobile.di

import com.kt.apps.core.tv.di.TVChannelModule

class MobileTVChannelModule: TVChannelModule() {
    override fun providesTimeout(): Long? = 5
}
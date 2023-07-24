package com.kt.apps.media.mobile.models

enum class LinkType {
    TV, Radio, IPTV
}
interface ILinkData {
    val type: LinkType
}
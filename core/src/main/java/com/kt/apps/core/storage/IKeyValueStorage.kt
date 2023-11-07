package com.kt.apps.core.storage

import com.kt.apps.core.extensions.ExtensionsConfig

interface IKeyValueStorage {
    fun <T> get(key: String, clazz: Class<T>): T
    fun <T : Any> save(key: String, value: T)
    fun <T, U> save(key: String, value: Map<T, U>)
    fun <T, U> get(key: String, clazz: Class<T>, clazz2: Class<U>): Map<T, U>

    fun <T> save(key: String, value: List<T>)
    fun <T> getList(key: String, clazz: Class<T>): List<T>
    fun remove(key: String)
}

fun IKeyValueStorage.saveLastRefreshExtensions(config: ExtensionsConfig) {
    this.save("${config.sourceUrl}_last_refresh_data", System.currentTimeMillis())
}

fun IKeyValueStorage.removeLastRefreshExtensions(config: ExtensionsConfig) {
    this.remove("${config.sourceUrl}_last_refresh_data")
}

fun IKeyValueStorage.getLastRefreshExtensions(config: ExtensionsConfig): Long {
    return try {
        this.get("${config.sourceUrl}_last_refresh_data", Long::class.java)
    } catch (e: Exception) {
        0L
    }
}

fun IKeyValueStorage.saveFCMToken(token: String) {
    this.save("FcmToken", token)
}

fun IKeyValueStorage.getFCMToken(): String {
    return try {
        this.get("FcmToken", String::class.java)
    } catch (e: Exception) {
        ""
    }
}

fun IKeyValueStorage.saveDefaultEpgUrl(url: String) {
    this.save("DefaultEpgUrl", url)
}

fun IKeyValueStorage.getDefaultEpgUrl(): String {
    return try {
        this.get("DefaultEpgUrl", String::class.java)
    } catch (e: Exception) {
        ""
    }
}

fun IKeyValueStorage.saveIsVipDb(value: Boolean) {
    this.save("IsVipDb", value)
}

fun IKeyValueStorage.getIsVipDb(): Boolean {
    return try {
        this.get("IsVipDb", Boolean::class.java)
    } catch (e: Exception) {
        false
    }
}
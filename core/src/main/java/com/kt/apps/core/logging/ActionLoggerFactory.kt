package com.kt.apps.core.logging

import com.kt.apps.core.BuildConfig
import javax.inject.Inject
import javax.inject.Provider

class ActionLoggerFactory @Inject constructor(
    private val creators: Map<Class<out IActionLogger>, @JvmSuppressWildcards Provider<IActionLogger>>
) {
    fun <T : IActionLogger> createLogger(loggerClass: Class<T>): T {
        Logger.d(this, "RequestCreator", loggerClass.name)

        for ((clazz) in creators) {
            Logger.d(this, "Creators", clazz.name)
        }
        var creator = creators[loggerClass]
        if (creator == null) {
            for ((key, value) in creators) {
                key.isAssignableFrom(loggerClass)
                creator = value
                break
            }
        }
        Logger.d(this, "LoggerCreator", creator!!::class.java.name)
        return creator.get()?.let {
            it as T
        } ?: throw IllegalStateException("No found factory for type: ${loggerClass.name}")
    }

    fun createLogger(): IActionLogger {
        return if (BuildConfig.DEBUG) {
            createLogger(LocalActionLoggerImpl::class.java)
        } else {
            createLogger(FirebaseActionLoggerImpl::class.java)
        }
    }

}
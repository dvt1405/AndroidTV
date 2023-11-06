package com.kt.apps.core.service

import android.content.SharedPreferences
import androidx.core.os.bundleOf
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.extensions.ParserExtensionsProgramSchedule
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.getDefaultEpgUrl
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.saveFCMToken
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

class CloudMessagingService : FirebaseMessagingService(), HasAndroidInjector {

    @Inject
    lateinit var keyValueStorage: IKeyValueStorage

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var actionLogger: IActionLogger

    @Inject
    lateinit var roomDataBase: RoomDataBase

    private val _compositeDisposable by lazy {
        CompositeDisposable()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.d(this, tag = "FCM", "FCM token: $token")
        if (this::keyValueStorage.isInitialized) {
            keyValueStorage.saveFCMToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d(
            this, tag = "FCM", "onMessageReceived: {" +
                    "messageId: ${message.messageId}, " +
                    "messageType: ${message.messageType}, " +
                    "sentTime: ${message.sentTime}, " +
                    "senderId: ${message.senderId}," +
                    "data: ${message.data}" +
                    "}"
        )
        actionLogger.log("FCMMsgReceive", bundleOf("fcmData" to "${message.data}"))

        message.data.forEach {
            when (it.key) {
                "type" -> {
                    when (it.value) {
                        "update" -> {
                            keyValueStorage.remove(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)
                            sharedPreferences.all.keys.forEach {
                                if (it.contains("_refresh_version")) {
                                    sharedPreferences.edit().remove(it).apply()
                                }
                            }
                            ParserExtensionsProgramSchedule.getInstance()?.clearCache()
                            Firebase.remoteConfig.fetchAndActivate()
                                .addOnSuccessListener { success ->
                                }
                                .addOnFailureListener {

                                }
                                .addOnCanceledListener {

                                }
                        }

                        "refresh" -> {
                        }

                        "clearCache" -> {
                            clearCacheProgram()
                        }

                        "clearCacheProgramCache" -> {
                            clearCacheProgram()
                        }
                    }
                }
            }
        }
    }

    private fun clearCacheProgram() {
        val defaultEpgUrl = keyValueStorage.getDefaultEpgUrl()
            .takeIf {
                it.isNotEmpty()
            } ?: return

        _compositeDisposable.add(
            roomDataBase.extensionsTVChannelProgramDao()
                .deleteProgramByConfig(
                    "DEFAULT",
                    defaultEpgUrl
                )
                .subscribe({
                    Logger.d(
                        this,
                        tag = "FCM",
                        "clearCacheProgramCache: success"
                    )
                }, {
                    Logger.e(
                        this,
                        tag = "FCM",
                        exception = it
                    )
                })
        )
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return (application as CoreApp).androidInjector()
    }
}
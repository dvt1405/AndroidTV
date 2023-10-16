package com.kt.apps.core.storage.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import com.kt.apps.core.utils.testReadJsonFile
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okio.IOException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDataBaseTest {
    lateinit var testDb: RoomDataBase
    lateinit var serverJson: JSONObject
    @Before
    fun setup() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDb = Room.inMemoryDatabaseBuilder(
            context, RoomDataBase::class.java
        ).build()
        val str = testReadJsonFile(context, "test_channel_list_tv.json")
        serverJson = JSONObject(str)
        val allChannel = serverJson.getJSONArray("AllChannels")
        val listChannel = mutableListOf<TVChannelDTO>()
        val listChannelUrl = mutableListOf<TVChannelDTO.TVChannelUrl>()
        for (i in 0 until allChannel.length()) {
            val jsonObject = allChannel.optJSONObject(i) ?: continue
            listChannel.add(
                TVChannelDTO(
                    jsonObject.optString("group"),
                    jsonObject.optString("thumb"),
                    jsonObject.optString("name"),
                    jsonObject.optString("src"),
                    jsonObject.optString("id"),
                    jsonObject.optString("name").lowercase()
                        .replaceVNCharsToLatinChars()
                        .removeAllSpecialChars()
                )
            )
            for (j in 0 until jsonObject.getJSONArray("urls").length()) {
                val urlObject = jsonObject.getJSONArray("urls").optJSONObject(j) ?: continue
                listChannelUrl.add(
                    TVChannelDTO.TVChannelUrl(
                        urlObject.optString("src"),
                        urlObject.optString("type"),
                        urlObject.optString("url"),
                        jsonObject.optString("id")
                    )
                )
            }
        }
        testDb.tvChannelDao().insertListChannel(listChannel)
            .blockingSubscribe {
                println("Insert list channel success")
            }
        testDb.tvChannelUrlDao().insert(listChannelUrl)
            .blockingSubscribe {
                println("Insert list channel url success")
            }
    }

    @After
    @Throws(IOException::class)
    fun after() {
        testDb.close()
    }

    @Test
    fun testRead() {
        testDb.tvChannelDao().getListChannelWithUrl()
            .test()
            .assertNoErrors()
            .assertValue {
                println("Size: ${it.size}")
                it.forEach {
                    println("${it.tvChannel}")
                    println("${it.urls}")
                }
                it.isNotEmpty()
            }
    }
}
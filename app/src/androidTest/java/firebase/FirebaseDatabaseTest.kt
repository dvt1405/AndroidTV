package firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.impl.MainTVDataSource
import com.kt.apps.core.tv.datasource.impl.MainTVDataSource.Companion.mapToListChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean


@RunWith(AndroidJUnit4::class)
class FirebaseDatabaseTest {

    private lateinit var app: FirebaseApp
    private lateinit var oldRef: DatabaseReference
    private lateinit var newRef: DatabaseReference
    private val supportGroups by lazy {
        listOf(
            TVChannelGroup.VTV,
            TVChannelGroup.HTV,
            TVChannelGroup.SCTV,
            TVChannelGroup.VTC,
            TVChannelGroup.THVL,
            TVChannelGroup.AnNinh,
            TVChannelGroup.HTVC,
            TVChannelGroup.DiaPhuong,
            TVChannelGroup.Intenational,
            TVChannelGroup.Kid,

            TVChannelGroup.VOV,
            TVChannelGroup.VOH,
        )
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseOptions.fromResource(context)
        app = FirebaseApp.initializeApp(context)!!
        newRef = FirebaseDatabase.getInstance(
            app,
            "https://xemtv-e551b-vip.asia-southeast1.firebasedatabase.app"
        )
            .reference
        oldRef = FirebaseDatabase.getInstance(app).reference
    }

    @Test
    fun testGetNewData() {
        var dataSnapshot: DataSnapshot? = null
        val isSuccess = AtomicBoolean(false)
        println("testGetData")
        newRef.child("AllChannels")
            .ref.get()
            .addOnSuccessListener {
                println("Success")
                println(it)
                dataSnapshot = it
                isSuccess.setRelease(true)
            }
            .addOnFailureListener {
                println("Fail")
                println(it.message)
                println(it)
                isSuccess.setRelease(true)
            }
        while (!isSuccess.get()) {
            Thread.sleep(500)
        }
        val childValue = dataSnapshot!!.getValue<List<MainTVDataSource.TVChannelFromDB?>>()
        println("Value - ${childValue?.size}")
        val tvList = childValue?.filterNotNull()
            ?.mapToListChannel()
            ?.sortedBy(ITVDataSource.sortTVChannel())
        assert(!tvList.isNullOrEmpty())
    }

    @Test
    fun testGetOldData() {
        var dataSnapshot: DataSnapshot? = null
        val isSuccess = AtomicBoolean(false)
        oldRef.child("AllChannels")
            .ref.get()
            .addOnSuccessListener {
                println("Success")
                println(it)
                dataSnapshot = it
                isSuccess.setRelease(true)
            }
            .addOnFailureListener {
                println("Fail")
                println(it.message)
                println(it)
                isSuccess.setRelease(true)
            }
        while (!isSuccess.get()) {
            Thread.sleep(500)
        }
        supportGroups.forEach {
            println("TV Group: $it")
            val childValue = dataSnapshot!!
                .child(it.name)
                .getValue<List<MainTVDataSource.TVChannelFromDB?>>()
            println("Value - ${childValue?.size}")
            val tvList = childValue?.filterNotNull()
                ?.mapToListChannel()
                ?.sortedBy(ITVDataSource.sortTVChannel())
            assert(!tvList.isNullOrEmpty())
        }
    }
}
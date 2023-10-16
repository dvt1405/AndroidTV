package com.kt.apps.core.utils

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset


@Throws(IOException::class, JSONException::class)
fun testReadJsonFile(context: Context, fileName: String): String {
    val inputStream: InputStream = context.assets.open(fileName)
    val size = inputStream.available()
    val buffer = ByteArray(size)
    inputStream.read(buffer)
    inputStream.close()
    return String(buffer, Charset.defaultCharset())
}
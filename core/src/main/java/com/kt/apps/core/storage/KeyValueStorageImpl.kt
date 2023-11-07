package com.kt.apps.core.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

open class KeyValueStorageImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : IKeyValueStorage {

    override fun <T> get(key: String, clazz: Class<T>): T {
        return when (clazz) {
            String::class.javaObjectType,
            String::class.javaPrimitiveType -> {
                sharedPreferences.getString(key, "") as T
            }

            Boolean::class.javaObjectType,
            Boolean::class.javaPrimitiveType -> {
                sharedPreferences.getBoolean(key, false) as T
            }

            Int::class.javaObjectType,
            Int::class.javaPrimitiveType -> {
                sharedPreferences.getInt(key, -1) as T
            }

            Float::class.javaObjectType,
            Float::class.javaPrimitiveType -> sharedPreferences.getFloat(key, -1f) as T

            Long::class.javaObjectType,
            Long::class.javaPrimitiveType -> sharedPreferences.getLong(key, -1) as T
            else -> {
                val strValue = sharedPreferences.getString(key, "")
                Gson().fromJson(strValue, clazz)
            }
        }
    }

    override fun <T : Any> save(key: String, value: T) {
        when (value::class.java) {
            String::class.javaObjectType,
            String::class.javaPrimitiveType -> {
                sharedPreferences.edit()
                    .putString(key, value as String)
                    .apply()
            }

            Boolean::class.javaObjectType,
            Boolean::class.javaPrimitiveType -> {
                sharedPreferences.edit()
                    .putBoolean(key, value as Boolean)
                    .apply()
            }

            Int::class.javaObjectType,
            Int::class.javaPrimitiveType -> {
                sharedPreferences.edit()
                    .putInt(key, value as Int)
                    .apply()
            }
            Float::class.javaObjectType,
            Float::class.javaPrimitiveType -> {
                sharedPreferences.edit()
                    .putFloat(key, value as Float)
                    .apply()
            }
            Long::class.javaObjectType, Long::class.javaPrimitiveType -> {
                sharedPreferences.edit()
                    .putLong(key, value as Long)
                    .apply()
            }

            else -> {
                val strValue = Gson().toJson(value)
                sharedPreferences.edit()
                    .putString(key, strValue)
                    .apply()
            }
        }
    }

    override fun <T, U> save(key: String, value: Map<T, U>) {
        sharedPreferences.edit().putString(key, Gson().toJson(value)).apply()
    }

    override fun <T, U> get(key: String, clazz: Class<T>, clazz2: Class<U>): Map<T, U> {
        return try {
            val type = TypeToken.getParameterized(Map::class.java, clazz, clazz2).type
            Gson().fromJson(sharedPreferences.getString(key, ""), type) as Map<T, U>
        } catch (_: Exception) {
            mapOf()
        }

    }

    override fun <T> save(key: String, value: List<T>) {
        sharedPreferences.edit().putString(key, Gson().toJson(value)).apply()
    }

    override fun <T> getList(key: String, clazz: Class<T>): List<T> {
        val type = TypeToken.getParameterized(List::class.java, clazz).type
        val gsonValue = sharedPreferences.getString(key, "")
        if (gsonValue?.isEmpty() != false) return listOf()
        return Gson().fromJson(gsonValue, type)
    }

    override fun remove(key: String) {
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }
}
package com.yuyin.demo.models

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSettingsTest {

    @Test
    fun testEquals() {
        val moshi: Moshi = Moshi.Builder().build()
        val localSettings = LocalSettings(0, mutableMapOf("zh" to listOf("abc","bcd")),"zh")
        val jsonAdapter: JsonAdapter<LocalSettings> = moshi.adapter(LocalSettings::class.java)
        val json = jsonAdapter.toJson(localSettings)
        val fileSettings = jsonAdapter.fromJson(json)
        assertEquals(fileSettings,localSettings)
    }
}
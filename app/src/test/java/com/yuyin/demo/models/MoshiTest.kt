package com.yuyin.demo.models

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class MoshiTest {

    private val moshi: Moshi = Moshi.Builder().build()


    @Test
    fun testLocalSettings() {
        val localSettings = LocalSettings(0, mutableMapOf("zh" to mutableListOf("abc","bcd")),"zh")
        val jsonAdapter: JsonAdapter<LocalSettings> = moshi.adapter(LocalSettings::class.java)
        val json = jsonAdapter.toJson(localSettings)
        val fileSettings = jsonAdapter.fromJson(json)
        assertEquals(fileSettings,localSettings)
    }

    @Test
    fun testLocalResult() {
        val localResult = LocalResult(listOf("string") as MutableList<String>, listOf(1), listOf(2) ,"audio", 2)
        val jsonAdapter: JsonAdapter<LocalResult> = moshi.adapter(LocalResult::class.java)
        val json = jsonAdapter.toJson(localResult)
        val speech = jsonAdapter.fromJson(json)
        assertEquals(localResult,speech)
    }
}
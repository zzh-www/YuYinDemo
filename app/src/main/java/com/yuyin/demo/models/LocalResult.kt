package com.yuyin.demo.models

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
data class LocalResult(
    var speechText: MutableList<String>,
    var start: List<Int>,
    var end: List<Int>,
    var audioFile: String,
    var resultType: Int
) {
    companion object {
        val emptyLocalResult = LocalResult(mutableListOf(), listOf(), listOf(), "", 0)

        fun fromJson(moshi: Moshi, file: File): LocalResult {
            val jsonAdapter: JsonAdapter<LocalResult> =
                moshi.adapter(LocalResult::class.java)
            return jsonAdapter.fromJson(file.readText()) ?: emptyLocalResult

        }
    }

    fun toJson(moshi: Moshi, file: File) {
        val jsonAdapter: JsonAdapter<LocalResult> =
            moshi.adapter(LocalResult::class.java)
        file.writeText(jsonAdapter.toJson(this))
    }

    fun toText(file: File) {
        file.outputStream().use {
            for (i in speechText.indices) {
                if (resultType > 0) {
                    it.write("from ${start[i]/1000f}s to ${end[i]/1000f}s\n".toByteArray())
                }
                it.write("${speechText[i]}\n".toByteArray())
            }
        }
    }
}

data class ResultItem(
    var speechText: String,
    var start: Int,
    var end: Int,
    var state: AudioPlay.AudioConfigState = AudioPlay.AudioConfigState.PLAY
)
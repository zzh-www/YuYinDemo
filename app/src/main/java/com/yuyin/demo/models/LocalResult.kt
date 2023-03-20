package com.yuyin.demo.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocalResult(var speechText: MutableList<String>, var start: List<Int>, var end: List<Int>, var audioFile: String, var resultType: Int) {
    companion object {
        val emptyLocalResult = LocalResult(mutableListOf(), listOf(), listOf(),"",0)
    }
}

data class ResultItem(var speechText: String, var start: Int, var end: Int, var state: AudioPlay.AudioConfigState = AudioPlay.AudioConfigState.PLAY)
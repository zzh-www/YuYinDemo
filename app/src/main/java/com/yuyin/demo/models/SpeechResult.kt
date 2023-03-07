package com.yuyin.demo.models

import java.nio.charset.StandardCharsets

data class SpeechResult(val textByte: ByteArray, var start: Int, var end: Int, val state: Int) {
    val text get() = String(textByte, StandardCharsets.UTF_8)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeechResult

        if (!textByte.contentEquals(other.textByte)) return false
        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }



    override fun toString(): String {
        return "text: $text start from $start end on $end state is $state"
    }

    override fun hashCode(): Int {
        var result = textByte.contentHashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}
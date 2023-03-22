package com.yuyin.demo.models

import com.yuyin.demo.utils.PCMToWAV
import com.yuyin.demo.utils.WAVHeader
import com.yuyin.demo.utils.YuYinUtil
import com.yuyin.demo.utils.writeToOutput
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.OutputStream

fun WAVHeader.writeHeader(encoded: OutputStream) {
    // WAVE RIFF header

    writeToOutput(encoded, chunk_id) // chunk id
    // 4 byte
    writeToOutput(encoded, chunk_size) // chunk size
    // 4 byte
    writeToOutput(encoded, format) // format
    // 4 byte

    // SUB CHUNK 1 (FORMAT)
    writeToOutput(encoded, sub_chunk) // subchunk 1 id
    // 4 byte
    writeToOutput(encoded, sub_chunkSize) // subchunk 1 size
    // 4 byte
    writeToOutput(encoded, audioFormat) // audio format (1 = PCM)
    // 2 byte
    writeToOutput(encoded, channelCount) // number of channelCount
    // 2 byte
    writeToOutput(encoded, sampleRate) // sample rate
    // 4 byte
    writeToOutput(encoded, byteRate) // byte rate
    // 4 byte
    writeToOutput(encoded, blockAlign) // block align
    // 2 byte
    writeToOutput(encoded, bitsPerSample) // bits per sample
    // 2 byte

    // SUB CHUNK 2 (AUDIO DATA)

    writeToOutput(encoded, data_chunk) // subchunk 2 id
    // 4 byte
    writeToOutput(encoded, data_chunkSize) // subchunk 2 size
    // 4 byte
}

class FileTest {

    @Test
    fun testPCMToWAV() {
        val pcmFile = File("./src/test/assets/tmp.pcm")
        // play with ffplay
        val newFile = File("./src/test/assets/tmp.wav")
        PCMToWAV(pcmFile, newFile, 1, YuYinUtil.RecordHelper.RECORDER_SAMPLERATE, 16)
        newFile.inputStream().use {
            val header = WAVHeader.generate(it)
            println(header)
        }
    }

    @Test
    fun testRedWAVFile() {
        val wavFile = File("./src/test/assets/tmpHeader.wav")
        val header = WAVHeader(
            "RIFF",
            36 + 22333,
            "WAVE",
            "fmt ",
            16,
            1,
            1,
            16000,
            32000,
            2,
            16,
            "data",
            22333
        )
        wavFile.outputStream().use {
            header.writeHeader(it)
        }
        wavFile.inputStream().use {
            val wavHeader = WAVHeader.generate(it)
            assertEquals(wavHeader, header)
        }
    }

    @Test
    fun testCompress() {
        YuYinUtil.compressFiles(
            listOf(File("./src/test/assets/tmp.pcm"), File("./src/test/assets/tmpHeader.wav")),
            File("./src/test/assets/tmp.zip")
        )
    }

}
package com.yuyin.demo.utils

import android.media.AudioFormat
import java.io.*

/**
 * @param input         raw PCM data
 * limit of file size for wave file: < 2^(2*4) - 36 bytes (~4GB)
 * @param output        wav保存文件
 * @param channelCount  声道数: 1 for mono, 2 for stereo, etc. 系统所定义的需要进行转换
 * @param sampleRate    采样率
 * @param bitsPerSample 样本位深度 例如 16 for PCM16 系统所定义需要转换
 * @throws IOException in event of an error between input/output files
 * @see [soundfile.sapp.org/doc/WaveFormat](http://soundfile.sapp.org/doc/WaveFormat/)
 * @see getRealChannelCount
 * @see getRealEncoding
 */
@Throws(IOException::class)
fun PCMToWAV(
    input: File,
    output: File?,
    channelCount: Int,
    sampleRate: Int,
    bitsPerSample: Int
) {

    val inputSize = input.length().toInt()

    val defaultBufferSize = 10 * 1024

    FileOutputStream(output).use { encoded ->

        // WAVE RIFF header

        writeToOutput(encoded, "RIFF") // chunk id
        // 4 byte
        writeToOutput(encoded, 36 + inputSize) // chunk size
        // 4 byte
        writeToOutput(encoded, "WAVE") // format
        // 4 byte

        // SUB CHUNK 1 (FORMAT)
        writeToOutput(encoded, "fmt ") // subchunk 1 id
        // 4 byte
        writeToOutput(encoded, 16) // subchunk 1 size
        // 4 byte
        writeToOutput(encoded, 1.toShort()) // audio format (1 = PCM)
        // 2 byte
        writeToOutput(encoded, channelCount.toShort()) // number of channelCount
        // 2 byte
        writeToOutput(encoded, sampleRate) // sample rate
        // 4 byte
        writeToOutput(encoded, getByteRate(channelCount, sampleRate, bitsPerSample)) // byte rate
        // 4 byte
        writeToOutput(encoded, (channelCount * bitsPerSample / 8).toShort()) // block align
        // 2 byte
        writeToOutput(encoded, bitsPerSample.toShort()) // bits per sample
        // 2 byte

        // SUB CHUNK 2 (AUDIO DATA)

        writeToOutput(encoded, "data") // subchunk 2 id
        // 4 byte
        writeToOutput(encoded, inputSize) // subchunk 2 size
        // 4 byte

        input.inputStream().use {
            copy(it, encoded, defaultBufferSize)
        }

    }

}

data class WAVHeader(
    val chunk_id: String,
    val chunk_size: Int,
    val format: String,
    val sub_chunk: String,
    val sub_chunkSize: Int,
    val audioFormat: Short,
    val channelCount: Short,
    val sampleRate: Int,
    val byteRate: Int,
    val blockAlign: Short,
    val bitsPerSample: Short,
    val data_chunk: String,
    val data_chunkSize: Int
) {
    /**
     * 获取WAV文件头，44字节长度
     */
    companion object {
        fun generate(input: InputStream): WAVHeader = WAVHeader(
            readFromInputToString(input),
            readFromInputToInt(input),
            readFromInputToString(input),
            readFromInputToString(input),
            readFromInputToInt(input),
            readFromInputToShort(input),
            readFromInputToShort(input),
            readFromInputToInt(input),
            readFromInputToInt(input),
            readFromInputToShort(input),
            readFromInputToShort(input),
            readFromInputToString(input),
            readFromInputToInt(input)
        )
    }
}


/**
 * Size of buffer used for transfer, by default
 */
private const val TRANSFER_BUFFER_SIZE = 10 * 1024

/**
 * Writes string in big endian form to an output stream
 *
 * @param output stream
 * @param data   string
 * @throws IOException
 */

@Throws(IOException::class)
fun writeToOutput(output: OutputStream, data: String) {
    for (element in data) {
        output.write(element.code)
    }
}

@Throws(IOException::class)
fun readFromInputToString(input: InputStream): String {
    val bytes = ByteArray(4)
    input.read(bytes)
    return String(bytes)
}

@Throws(IOException::class)
fun readFromInputToShort(input: InputStream): Short {
    val b = ByteArray(2)
    input.read(b)
    val int1 = (b[0].toInt() and 0xff) shl 0
    val int2 = (b[1].toInt() and 0xff) shl 8
    return (int1 or int2).toShort()
}

fun readFromInputToInt(input: InputStream): Int {
    val b = ByteArray(4)
    input.read(b)
    val int1 = (b[0].toInt() and 0xff) shl 0
    val int2 = (b[1].toInt() and 0xff) shl 8
    val int3 = (b[2].toInt() and 0xff) shl 16
    val int4 = (b[3].toInt() and 0xff) shl 24
    return int1 or int2 or int3 or int4
}

@Throws(IOException::class)
fun writeToOutput(output: OutputStream, data: Int) {
    output.write(data shr 0)
    output.write(data shr 8)
    output.write(data shr 16)
    output.write(data shr 24)
}

@Throws(IOException::class)
fun writeToOutput(output: OutputStream, data: Short) {
    output.write(data.toInt() shr 0)
    output.write(data.toInt() shr 8)
}

@Throws(IOException::class)
fun copy(source: InputStream, output: OutputStream): Long {
    return copy(source, output, TRANSFER_BUFFER_SIZE)
}

@Throws(IOException::class)
fun copy(source: InputStream, output: OutputStream, bufferSize: Int): Long {
    var read = 0L
    val buffer = ByteArray(bufferSize)
    var n: Int
    while (source.read(buffer).also { n = it } != -1) {
        output.write(buffer, 0, n)
        read += n.toLong()
    }
    return read
}

fun short2byte(sData: ShortArray): ByteArray {
    val shortArrsize = sData.size
    val bytes = ByteArray(shortArrsize * 2)
    for (i in 0 until shortArrsize) {
        bytes[i * 2] = (sData[i].toInt() and 0x00FF).toByte()
        bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
        sData[i] = 0
    }
    return bytes
}

/**
 * 获取当前录音的采样位宽 单位bit
 *
 * @return 采样位宽 0: error
 */
fun getRealEncoding(format: Int) = when (format) {
    AudioFormat.ENCODING_PCM_8BIT -> 8
    AudioFormat.ENCODING_PCM_16BIT -> 16
    else -> 0
}

/**
 * 当前的声道数
 *
 * @return 声道数： 0：error
 */
fun getRealChannelCount(channel: Int) = when (channel) {
    AudioFormat.CHANNEL_IN_MONO -> 1
    AudioFormat.CHANNEL_IN_STEREO -> 2
    else -> 0
}

fun getByteRate(
    channelCount: Int,
    sampleRate: Int,
    bitsPerSample: Int
) = channelCount * sampleRate * bitsPerSample / 8

package com.yuyin.demo.models

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_AUDIO_ENCODING
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_SAMPLERATE

object AudioPlay {
    enum class AudioConfigState {
        PLAY,
        STOP,
    }

    data class AudioConfig(val start: Int, val end: Int, val State: AudioConfigState, val id: Int)


    var isPlay = false


    private var audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val audioFormat: AudioFormat = AudioFormat.Builder()
        .setSampleRate(RECORDER_SAMPLERATE)
        .setEncoding(RECORDER_AUDIO_ENCODING)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO,)
        .build()

    val miniBufferSize: Int =
        AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING)

    val audioTrack: AudioTrack = AudioTrack.Builder()
        .setAudioFormat(audioFormat)
        .setAudioAttributes(audioAttributes)
        .setBufferSizeInBytes(miniBufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
}
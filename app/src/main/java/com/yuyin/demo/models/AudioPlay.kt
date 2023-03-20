package com.yuyin.demo.models

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_AUDIO_ENCODING
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_SAMPLERATE
import java.io.File

object AudioPlay {
    enum class AudioConfigState {
        PLAY,
        STOP,
    }

    data class AudioConfig(val start: Int, val end: Int, val State: AudioConfigState, val id: Int)

    var audioResource = File("")

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

    private val miniBufferSize: Int =
        AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING)

    private val audioTrack: AudioTrack = AudioTrack.Builder()
        .setAudioFormat(audioFormat)
        .setAudioAttributes(audioAttributes)
        .setBufferSizeInBytes(miniBufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    private var timeStamp = AudioTimestamp()

    fun initAudioTrack(file: File): Boolean {
        if (file.exists()) {
            audioResource = file
            return true
        }
        return false
    }

    fun acceptConfig(audioConfig: AudioConfig) {
        when (audioConfig.State) {
            AudioConfigState.STOP -> stop()
            AudioConfigState.PLAY -> play(audioConfig.start, audioConfig.end)
        }
    }

    fun stop() {
        isPlay = false
    }

    fun play(start: Int, end: Int) {
        if (isPlay) {
            isPlay = false
        }
    }
}
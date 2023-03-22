package com.yuyin.demo.viewmodel

import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.LocalResult
import com.yuyin.demo.utils.WAVHeader
import com.yuyin.demo.view.edit.ResultAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class EditViewModel : ViewModel() {
    private val TAG = "EditViewModel"
    var localResult: LocalResult = LocalResult.emptyLocalResult
    val audioConfig = MutableSharedFlow<AudioPlay.AudioConfig>()
    val audioItemNotification = MutableSharedFlow<ResultAdapter.Notification>()
    var scope = CoroutineScope(Job() + Dispatchers.IO)
    var audioResource = File("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            audioConfig.collect {
                when (it.State) {
                    AudioPlay.AudioConfigState.PLAY -> {
                        if (AudioPlay.isPlay) {
                            scope.coroutineContext[Job]?.cancel()
                            AudioPlay.isPlay = false
                        }
                        audioItemNotification.emit(ResultAdapter.Notification(it.id,true))
                        if (audioResource.exists() && !AudioPlay.isPlay) {
                            // 新建上下文
                            scope = CoroutineScope(Job() + Dispatchers.IO)
                            scope.launch {
                                AudioPlay.isPlay = true
                                audioResource.inputStream().use { input ->
                                    // 读取文件头部
                                    val header = WAVHeader.generate(input)
                                    Log.i(TAG, "wav header: $header")
                                    // 每一百毫秒音频的字节数
                                    val bytesPerMS = header.byteRate / 10
                                    // start 向下取整
                                    val start = kotlin.math.floor(it.start / 100f).toInt() * bytesPerMS
                                    val end = kotlin.math.ceil(it.end / 100f).toInt() * bytesPerMS
                                    Log.i(TAG, "bytesPerMS $bytesPerMS start $start end $end")
                                    // 跳转到开始
                                    var buffer = ByteArray(bytesPerMS)
                                    var read = 0
                                    for (i in 0 until start / bytesPerMS) {
                                        if (input.read(buffer) < 0) {
                                            AudioPlay.isPlay = false
                                            audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                                            this.coroutineContext[Job]?.cancel("未跳转到音频开始，文件已经结束")
                                        }
                                    }
                                    // 已经播放的字节数
                                    var duration = 0
                                    buffer = ByteArray(AudioPlay.miniBufferSize)
                                    while (isActive && AudioPlay.isPlay) {
                                        read = input.read(buffer)
                                        if (read < 0) {
                                            // 到文件尾部
                                            break
                                        }
                                        else if ((start + duration) > end) {
                                            // 到该段音频末尾
                                            break
                                        }
                                        duration += read
                                        AudioPlay.audioTrack.play()
                                        when(AudioPlay.audioTrack.write(buffer,0,read)) {
                                            AudioTrack.ERROR_INVALID_OPERATION -> {
                                                audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                                                Log.e(TAG,"play failed, ERROR_INVALID_OPERATION")
                                                this.coroutineContext[Job]?.cancel("播放错误")
                                            }
                                            AudioTrack.ERROR_BAD_VALUE -> {
                                                audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                                                Log.e(TAG,"play failed ERROR_BAD_VALUE")
                                                this.coroutineContext[Job]?.cancel("播放错误")
                                            }
                                            AudioTrack.ERROR_DEAD_OBJECT -> {
                                                audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                                                Log.e(TAG,"play failed ERROR_DEAD_OBJECT")
                                                this.coroutineContext[Job]?.cancel("播放错误")
                                            }
                                            else -> {
                                                Log.i(TAG, "play ${duration / header.byteRate} config: $it")
                                            }
                                        }
                                    }
                                }
                                audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                                Log.i(TAG, "stop $it")
                            }
                        } else {
                            audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                            Log.e(
                                TAG,
                                "file not exit ${audioResource.absolutePath} or is not in init type: ${AudioPlay.isPlay}"
                            )
                        }
                    }
                    AudioPlay.AudioConfigState.STOP -> {
                        if (AudioPlay.isPlay) {
                            scope.coroutineContext[Job]?.cancel()
                            Log.i(TAG, "cancel $it")
                            AudioPlay.audioTrack.pause()
                            AudioPlay.audioTrack.flush()
                            AudioPlay.isPlay = false
                            audioItemNotification.emit(ResultAdapter.Notification(it.id,false))
                        }
                    }
                }
            }
        }
    }
}
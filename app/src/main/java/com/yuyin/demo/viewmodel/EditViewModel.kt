package com.yuyin.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.LocalResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class EditViewModel : ViewModel() {
    private val TAG = "EditViewModel"
    var localResult: LocalResult = LocalResult.emptyLocalResult
    val audioConfig = MutableSharedFlow<AudioPlay.AudioConfig>()
    var scope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            audioConfig.collect {
                when (it.State) {
                    AudioPlay.AudioConfigState.PLAY -> {
                        if (AudioPlay.isPlay) {
                            scope.coroutineContext[Job]?.cancel()
                        }
                        // 新建上下文
                        scope = CoroutineScope(Job() + Dispatchers.IO)
                        scope.launch {
                            AudioPlay.isPlay = true
                            while (isActive) {
                                TODO("play music")
                                delay(100)
                                Log.i(TAG, "play $it")
                            }
                            Log.i(TAG, "stop $it")
                        }
                    }
                    AudioPlay.AudioConfigState.STOP -> {
                        scope.coroutineContext[Job]?.cancel()
                        Log.i(TAG, "cancel $it")
                        AudioPlay.isPlay = false
                    }
                }
            }
        }
    }
}
package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.models.OnNativeAsrModelCall
import com.yuyin.demo.models.SpeechResult
import com.yuyin.demo.view.speech.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import com.yuyin.demo.YuYinUtil.YuYinLog as Log

open class RunningAsrViewModel : ViewModel() {
    private val tag = "RunningAsrViewModel"

    var recordState = false
    var miniBufferSize = 0
    val MAX_QUEUE_SIZE = 2500

    // 100 seconds audio, 1 / 0.04 * 100
    val SAMPLE_RATE = 16000 // The sampling rate

    var asrState = false
    var isModelInit = MutableStateFlow(false) // 热流不管是否有人订阅都会更新
    var isModelReset = MutableStateFlow(false)
    var isModelFinish = MutableStateFlow(true)
    var change_senor = false
    var offsetOfTime = 0

    // 滚动视图
    val speechList: ArrayList<SpeechResult> = arrayListOf()

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList,this)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results:MutableStateFlow<SpeechResult> = MutableStateFlow(SpeechResult("".toByteArray(),0,0,1))

    val hotResult= MutableStateFlow("")

    val canScroll = MutableStateFlow(true)

    val asrListener = object : OnNativeAsrModelCall {
        override fun onSpeechResultReceive(data: SpeechResult) {
            viewModelScope.launch(Dispatchers.Default) {
                data.start += offsetOfTime
                data.end += offsetOfTime
                results.emit(data)
                Log.i(tag,"onSpeechResultReceive $data")
            }
        }

        override fun onSpeechResultPartReceive(data: ByteArray) {
            viewModelScope.launch(Dispatchers.Default) {
                hotResult.emit(String(data,StandardCharsets.UTF_8))
                Log.i(tag,"onSpeechResultPartReceive ${String(data)}")
            }
        }

        override fun onModelInit(isInit: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelInit.emit(isInit)
                Log.i(tag,"onModelInit $isInit")
            }
        }

        override fun onModelReset(isReset: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelReset.emit(isReset)
                Log.i(tag,"onModelReset $isReset")
            }
        }

        override fun onModelFinish(isFinish: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelFinish.emit(isFinish)
                isModelFinish.value
                Log.i(tag,"onModelFinish $isFinish")
            }
        }

    }


    init {
        // 一些状态标志更新，不直接更新界面才应该使用 viewModelScope
        viewModelScope.launch(Dispatchers.Main) {
            adapter.isFocus.collect {
                if (it) {
                    // 编辑态时不可滚动
                    canScroll.emit(false)
                }
            }
        }
    }

    fun produceAudio(record: AudioRecord) {
        viewModelScope.launch(Dispatchers.Default) {
            flow {
                while (recordState) {
                    val buffer = ShortArray(miniBufferSize / 2)
                    val read = record.read(buffer, 0, buffer.size)
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        emit(buffer)
                    }
                }
            }.catch {
                Log.e(tag, it.message ?: "error in audio flow")
            }.buffer(MAX_QUEUE_SIZE).collect {
                Recognize.acceptWaveform(it)
            }
        }
    }

    fun updateOffsetTime() {
        offsetOfTime = if (speechList.isEmpty()) {
            0
        } else {
            speechList.last().end
        }
        Log.i(tag,"update offsetOfTime $offsetOfTime")
    }

}
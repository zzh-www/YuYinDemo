package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.models.OnNativeAsrModelCall
import com.yuyin.demo.models.SpeechResult
import com.yuyin.demo.utils.YuYinUtil
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.miniBufferSize
import com.yuyin.demo.utils.getByteRate
import com.yuyin.demo.utils.getRealChannelCount
import com.yuyin.demo.utils.getRealEncoding
import com.yuyin.demo.view.speech.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

open class RunningAsrViewModel : ViewModel() {
    private val tag = "RunningAsrViewModel"

    var recordState = false
    val MAX_QUEUE_SIZE = 2500

    var asrState = false
    var isModelInit = MutableStateFlow(false) // 热流不管是否有人订阅都会更新
    var isModelReset = MutableStateFlow(false)
    var isModelFinish = MutableStateFlow(false)
    val audioData = MutableSharedFlow<ShortArray>()
    var change_senor = false
    var offsetOfTime = 0
    var needToSaveAudio = false
    var needToShowTime: Boolean = false

    // 滚动视图
    val speechList: ArrayList<SpeechResult> = arrayListOf()

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList, this)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results =
        MutableSharedFlow<SpeechResult>()

    val hotResult = MutableStateFlow("")

    val canScroll = MutableStateFlow(true)

    val asrListener = object : OnNativeAsrModelCall {
        override fun onSpeechResultReceive(data: SpeechResult) {
            viewModelScope.launch(Dispatchers.Default) {
                data.start += offsetOfTime
                data.end += offsetOfTime
                results.emit(data)
                Log.i(tag, "onSpeechResultReceive $data")
            }
        }

        override fun onSpeechResultPartReceive(data: ByteArray) {
            viewModelScope.launch(Dispatchers.Default) {
                hotResult.emit(String(data, StandardCharsets.UTF_8))
                Log.i(tag, "onSpeechResultPartReceive ${String(data)}")
            }
        }

        override fun onModelInit(isInit: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelInit.emit(isInit)
                Log.i(tag, "onModelInit $isInit")
            }
        }

        override fun onModelReset(isReset: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelReset.emit(isReset)
                Log.i(tag, "onModelReset $isReset")
            }
        }

        override fun onModelFinish(isFinish: Boolean) {
            viewModelScope.launch(Dispatchers.Default) {
                isModelFinish.emit(isFinish)
                Log.i(tag, "onModelFinish $isFinish")
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
                        if (needToSaveAudio) {
                            audioData.emit(buffer)
                        }
                    }
                }
            }.catch {
                Log.e(tag, it.message ?: "error in audio flow")
            }.buffer(MAX_QUEUE_SIZE).collect {
                Recognize.acceptWaveform(it)
            }
        }
    }

    fun updateOffsetTime(pcmFileLength: Long) {
        // 已经录制多少毫秒 = 总字节数 / (当前格式每秒字节数) * 1000
        offsetOfTime = (pcmFileLength / getByteRate(
            getRealChannelCount(YuYinUtil.RecordHelper.RECORDER_CHANNELS),
            YuYinUtil.RecordHelper.RECORDER_SAMPLERATE,
            getRealEncoding(YuYinUtil.RecordHelper.RECORDER_AUDIO_ENCODING)
        )).toInt() * 1000
        Log.i(tag, "update offsetOfTime $offsetOfTime")
    }

}
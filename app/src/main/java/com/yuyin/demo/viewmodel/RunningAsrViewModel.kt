package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.view.speech.SpeechText
import com.yuyin.demo.view.speech.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

open class RunningAsrViewModel : ViewModel() {
    private val tag = "RunningRecordViewModel"

    var recordState = false
    var miniBufferSize = 0
    val MAX_QUEUE_SIZE = 2500

    // 100 seconds audio, 1 / 0.04 * 100
    val SAMPLE_RATE = 16000 // The sampling rate

    var asrState = false
    var change_senor = false

    // 滚动视图
    val speechList: ArrayList<SpeechText> = arrayListOf()

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList,this)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results = MutableStateFlow("")

    val canScroll = MutableStateFlow(false)


    init {
        viewModelScope.launch(Dispatchers.Default) {
            adapter.isEdit.collect {
                canScroll.emit(!it)
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

    fun getTextFlow() {
        viewModelScope.launch(Dispatchers.Default) {
            flow {
                var length = 0
                while (asrState) {
                    try {
                        val result = Recognize.result
                        if (result.isNotEmpty() && result.length != length) {
                            emit(result)
                            length = result.length
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "error in decode : ${e.message}")
                    }
                }
            }.collect {
                Log.i(tag, "result: $it")
                results.value = it
            }
        }
    }

    // 或许考虑不使用热流 避免过于频繁更新界面
    fun updateFlow(flowText: TextView, recyclerView: RecyclerView, hotText:TextView) {
        viewModelScope.launch(Dispatchers.Main) {
            results.collect {
                flowText.text = it.removeSuffix("<end>")
                if (it.endsWith("<end>")) {
                    updateSpeechList(recyclerView,it.removeSuffix("<end>"))
                } else {
                    hotText.text = it
                    val offset = hotText.lineCount * hotText.lineHeight
                    if (offset > hotText.height) {
                        hotText.scrollTo(0, offset - hotText.height)
                    }
                }
            }
        }
    }

    fun updateSpeechList(recyclerView: RecyclerView, text:String) {
        val position = speechList.size - 1
        speechList.add(SpeechText(text)) // add new para
        adapter.notifyItemInserted(position + 1)
        if (canScroll.value) {
            recyclerView.scrollToPosition(position + 1)
        }
    }

}
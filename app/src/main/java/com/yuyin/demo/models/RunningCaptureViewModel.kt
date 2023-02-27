package com.yuyin.demo.models

import android.media.AudioRecord
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobvoi.wenet.MediaCaptureService
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.view.speech.SpeechText
import com.yuyin.demo.view.speech.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class RunningCaptureViewModel : ViewModel() {
    private val tag = "RunningCaptureViewModel"

    // record
    lateinit var record: AudioRecord
    var recordState = false
    var miniBufferSize = MediaCaptureService.miniBufferSize

    var asrState = false
    var change_senor = false

    // 滚动视图
    val speechList: ArrayList<SpeechText> = arrayListOf()

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results = MutableStateFlow("")

    fun produceAudio() {
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
                Log.e(tag, it.message ?: "error in audioflow")
            }.buffer(MediaCaptureService.MAX_QUEUE_SIZE).collect {
                Recognize.acceptWaveform(it)
            }
        }
    }

    fun getTextFlow() {
        viewModelScope.launch(Dispatchers.Default) {
            var length = 0
            flow {
                while (asrState) {
                    try {
                        val result = Recognize.result
                        if (result.length != length && result.isNotEmpty()) {
                            emit(result)
                            length = result.length
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "error in decode : ${e.message}")
                    }
                }
            }.collect {
                results.value = it
            }
        }
    }

    // 或许考虑不使用热流 避免过于频繁更新界面
    fun updateFlow(flowText: TextView, recyclerView: RecyclerView, hotText:TextView) {
        viewModelScope.launch(Dispatchers.Main) {
            results.collect {
                flowText.text = it.removeSuffix("<end>")
                val position = speechList.size - 1
                if (it.endsWith("<end>")) {
                    speechList.add(SpeechText(it.removeSuffix("<end>"))) // add new para
                    adapter.notifyItemInserted(position + 1)
                    recyclerView.scrollToPosition(position + 1)
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

}
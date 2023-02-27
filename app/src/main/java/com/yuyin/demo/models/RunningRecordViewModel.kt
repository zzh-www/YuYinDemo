package com.yuyin.demo.models

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.random.Random

class RunningRecordViewModel : ViewModel() {
    private val tag = "RunningRecordViewModel"

    // record
    var record: AudioRecord? = null
    var recordState = false
    var miniBufferSize = 0
    private val MAX_QUEUE_SIZE = 2500

    // 100 seconds audio, 1 / 0.04 * 100
    val SAMPLE_RATE = 16000 // The sampling rate

    var asrState = false
    var change_senor = false

    // 滚动视图
    val speechList: ArrayList<SpeechText> = arrayListOf()

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results = MutableStateFlow("")


    // debug
    var random = Random(11)

    fun produceAudio() {
        viewModelScope.launch(Dispatchers.Default) {
            flow {
                while (recordState) {
                    val buffer = ShortArray(miniBufferSize / 2)
                    val read = record?.read(buffer, 0, buffer.size)
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
            val i = random.nextInt()
            flow {
                while (asrState) {
                    try {
                        val result = Recognize.result
                        if (result != "") {
                            emit(result)
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

    fun updateFlow(flowText: TextView, recyclerView: RecyclerView, hotText:TextView) {
        viewModelScope.launch(Dispatchers.Main) {
            results.collect {
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
                flowText.text = it.removeSuffix("<end>")
            }
        }
    }

}
package com.yuyin.demo.models

import android.media.AudioRecord
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.SpeechText
import com.yuyin.demo.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RunningRecordViewModel : ViewModel() {
    private val LOGTAG = "RunningRecordViewModel"

    // record
    lateinit var record: AudioRecord
    var recordState = false
    private var miniBufferSize = 0
    private val MAX_QUEUE_SIZE = 2500
    // 100 seconds audio, 1 / 0.04 * 100

    var asrState = false
    var change_senor = false

    // 滚动视图
    val speechList: ArrayList<SpeechText> = arrayListOf(SpeechText(""))

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results = MutableStateFlow("Hi")


    @OptIn(InternalCoroutinesApi::class)
    fun produceAudio() {
        viewModelScope.launch(Dispatchers.Default) {
            val audioFlow = flow {
                while (recordState) {
                    val buffer = ShortArray(miniBufferSize / 2)
                    val read = record.read(buffer, 0, buffer.size)
                    try {
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            emit(buffer)
                        }
                    } catch (e: InterruptedException) {
                        Log.e(LOGTAG, e.message!!)
                    }
                }
            }.buffer(MAX_QUEUE_SIZE).collect {
                Recognize.acceptWaveform(it)
//                Log.i(LOGTAG, "$it")
            }
        }
    }

    fun getTextFlow() {
        viewModelScope.launch(Dispatchers.Default) {
            val resFlow = flow {
                var i = Random(11)
                while (asrState) {
                emit(Recognize.getResult())
                }
            }.collect {
                results.value = it.also {
                    Log.i(LOGTAG, it)
                }
            }
        }
    }

    fun updateFlow(flowText: TextView, recyclerView: RecyclerView) {
        viewModelScope.launch(Dispatchers.Main) {
            results.collect {
                flowText.text = it
                if (it.endsWith(" ")) {
                    speechList[speechList.size - 1].text = it
                    adapter.notifyItemChanged(speechList.size - 1)
                    speechList.add(SpeechText(" ")) // add new para
                    adapter.notifyItemInserted(speechList.size - 1)
                    recyclerView.scrollToPosition(speechList.size-1)
                } else {
                    speechList[speechList.size - 1].text = it // update latest para
                    adapter.notifyItemChanged(speechList.size - 1)
                }
            }
        }
    }

}
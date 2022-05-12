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
import com.yuyin.demo.SpeechText
import com.yuyin.demo.SpeechTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class RunningCaptureViewModel : ViewModel() {
    private val LOGTAG = "RunningCaptureViewModel"

    // record
    lateinit var record: AudioRecord
    var recordState = false
    var miniBufferSize = MediaCaptureService.miniBufferSize

    var asrState = false
    var change_senor = false

    // 滚动视图
    var speechList: ArrayList<SpeechText> = arrayListOf(SpeechText(""))

    //    private lateinit var recyclerView: RecyclerView
    var adapter: SpeechTextAdapter = SpeechTextAdapter(speechList)

    lateinit var linearLayoutManager: LinearLayoutManager

    val results = MutableStateFlow("Hi")


    // debug
    var random = Random(11)

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
                Log.e(LOGTAG, it.message ?: "error in audioflow")
            }.buffer(MediaCaptureService.MAX_QUEUE_SIZE).collect {
                Recognize.acceptWaveform(it)
//                Log.i(LOGTAG, "${it.size} : size of audio")
//                Log.i(LOGTAG, "$it")
            }
        }
    }

    fun getTextFlow() {
        viewModelScope.launch(Dispatchers.Default) {
            val i = random.nextInt()
            flow {
                while (asrState) {
//                    var result = "....${random.nextInt()}"
                    try {
                        val result = Recognize.getResult()
                        if (result != "")
                            emit(result)
//                        Log.d(LOGTAG,"decode $i")
                    } catch (e: Exception) {
                        Log.e(LOGTAG, "error in decode : ${e.message}")
                    }
                }
            }.collect {
                results.value = it
//                Log.i(LOGTAG, "collect in decode $it $i")
            }
        }
    }

    // 或许考虑不使用热流 避免过于频繁更新界面
    fun updateFlow(flowText: TextView, recyclerView: RecyclerView) {
        viewModelScope.launch(Dispatchers.Main) {
            results.collect {
                flowText.text = it
                val position = speechList.size - 1
                if (it.endsWith(" ")) {
                    speechList[position].text = it
                    adapter.notifyItemChanged(position)
                    speechList.add(SpeechText(" ")) // add new para
                    adapter.notifyItemInserted(position+1)
                    recyclerView.scrollToPosition(position+1)
                } else {
                    speechList[position].text = it // update latest para
                    adapter.notifyItemChanged(position)
                }
            }
        }
    }

}
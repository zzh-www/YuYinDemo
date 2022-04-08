package com.yuyin.demo.models

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mobvoi.wenet.MediaCaptureService.mcs_Binder
import com.yuyin.demo.RuningCapture
import com.yuyin.demo.SpeechText
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class YuyinViewModel : ViewModel() {


    var dic_path = ""
    var model_path = ""


    //    private val speechList: List<SpeechText> = ArrayList()
    val results: MutableLiveData<ArrayList<SpeechText>> = MutableLiveData<ArrayList<SpeechText>>()

    fun getResultsSize() = results.value?.size


    private val MAX_QUEUE_SIZE = 2500
    // 100 seconds audio, 1 / 0.04 * 100

    val bufferQueue: BlockingQueue<ShortArray> =
        ArrayBlockingQueue(MAX_QUEUE_SIZE)

    var startRecord = false
    var startAsr = false
    var change_senor = false

    lateinit var context: Activity

    var mBound = false
    var mcs_binder: mcs_Binder? = null
    lateinit var m_actionReceiver: RuningCapture.CaptureAudioReceiver


}
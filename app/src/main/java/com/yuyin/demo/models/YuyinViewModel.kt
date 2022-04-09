package com.yuyin.demo.models

import android.media.AudioRecord
import androidx.lifecycle.ViewModel

class YuyinViewModel : ViewModel() {


    var dic_path = ""
    var model_path = ""

    var startRecord = false
    var startAsr = false
    var change_senor = false

    lateinit var recorder: AudioRecord

}
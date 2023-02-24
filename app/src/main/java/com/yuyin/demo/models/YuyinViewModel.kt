package com.yuyin.demo.models

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import com.yuyin.demo.Main.Companion.tag

class YuyinViewModel : ViewModel() {


    var dic_path = ""
    var model_path = ""

    var startRecord = false
    var startAsr = false
    var change_senor = false

    var recorder: AudioRecord? = null

    var CurrentView = tag

    var bottomHeight: Int = 0

}
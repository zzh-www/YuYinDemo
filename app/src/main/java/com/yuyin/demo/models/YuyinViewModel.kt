package com.yuyin.demo.models

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import com.yuyin.demo.Main.Companion.tag

class YuyinViewModel : ViewModel() {


    var dic_path = ""

    var model_path = ""

    var recorder: AudioRecord? = null

}
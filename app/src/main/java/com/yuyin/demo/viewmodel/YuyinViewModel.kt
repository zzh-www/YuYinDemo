package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import androidx.lifecycle.ViewModel

class YuyinViewModel : ViewModel() {


    var dic_path = ""

    var model_path = ""

    var recorder: AudioRecord? = null

}
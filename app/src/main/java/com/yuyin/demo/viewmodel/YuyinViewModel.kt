package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import com.yuyin.demo.models.LocalSettings
import java.nio.file.Path
import kotlin.io.path.Path

class YuyinViewModel : ViewModel() {


    val dicPath: String
        get() {
            return settings.dictPath()
        }

    val modelPath: String
        get() {
            return settings.modelPath()
        }

    var yuYinDirPath = Path("")

    var settingProfilePath: Path = Path("")

    var recorder: AudioRecord? = null

    lateinit var settings: LocalSettings

    lateinit var newSettings: LocalSettings

}
package com.yuyin.demo.viewmodel

import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import com.yuyin.demo.models.LocalSettings
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class YuyinViewModel : ViewModel() {


    val dicPath: String
        get() {
            return settings.dictPath()
        }

    val modelPath: String
        get() {
            return settings.modelPath()
        }

    // 主目录
    var yuYinDirPath = Path("")

    // 设置文件路径
    var settingProfilePath: Path = Path("")

    // 数据文件目录
    var yuYinDataDir = Path("")

    // 临时文件目录
    val yuYinTmpDir: File
        get() = File(
            yuYinDirPath.absolutePathString(), "tmp"
        )

    val pcmTempFile: File get() = File(yuYinTmpDir.absolutePath, "tmp.pcm")

    var recorder: AudioRecord? = null

    val moshi: Moshi = Moshi.Builder().build()

    lateinit var settings: LocalSettings

    lateinit var newSettings: LocalSettings

}
package com.yuyin.demo.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object YuYinUtil {


    const val CaptureAudio_ALL = "CaptureAudioService"
    const val CaptureAudio_START = "CaptureAudioServiceSTART"
    const val CaptureAudio_START_ASR = "CaptureAudio_START_ASR"
    const val CaptureAudio_STOP = "CaptureAudio_STOP"
    const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
    const val EXTRA_CaptureAudio_NAME = "CaptureAudio_NAME"
    const val m_CREATE_SCREEN_CAPTURE = 1001
    const val EXTRA_ACTION_NAME = "ACTION_NAME"
    const val ACTION_ALL = "ALL"
    const val ACTION_START = "ACTION_START"
    const val ACTION_STOP = "ACTION_STOP"
    const val ACTION_START_RECORDING = "CaptureAudio_START_RECORDING"
    const val ACTION_STOP_RECORDING = "CaptureAudio_STOP_RECORDING"
    const val ACTION_STOP_RECORDING_From_Notification =
        "ACTION_STOP_RECORDING_From_Notification"
    const val ACTION_STOP_RECORDING_To_Main = "CaptureAudio_STOP_RECORDING_To_Main"
    const val ACTION_START_RECORDING_From_Notification =
        "CaptureAudio_START_RECORDING_From_Notification"

    // view
    private const val LOG_TAG = "YUYIN_RECORD"


    // 所需请求的权限
    val appPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE
    )
    const val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    @JvmStatic
    fun save_file(context: Context, text: String, title: String? = null) {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
        val dateStr = sdf.format(Date(timeStamp.toString().toLong()))
        val type = ".txt"
        val dir_path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
        var file: File?
        if (title.isNullOrBlank()) {
            file =
                File(dir_path.absoluteFile.toString() + File.separator + "YuYin" + File.separator + dateStr + type)
        } else {
            file =
                File(dir_path.absoluteFile.toString() + File.separator + "YuYin" + File.separator + title + type)
            if (file.exists()) {
                file =
                    File(dir_path.absoluteFile.toString() + File.separator + "YuYin" + File.separator + title + "_" + dateStr + type)
            }
        }
        try {
            if (file.createNewFile()) {
                FileOutputStream(file.absolutePath).use { op ->
                    op.write(
                        text.toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getFileName(dir: String, title: String? = null):Pair<File,File> {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
        val dateStr = sdf.format(Date(timeStamp.toString().toLong()))
        val type = ".json"
        val audioType = ".wav"
        val dir_path = dir
        var file: File?
        var audioFile:File?
        if (title.isNullOrBlank()) {
            file =
                File(dir_path ,dateStr + type)
            audioFile =
                File(dir_path,dateStr + audioType)
        } else {
            file =
                File(dir_path, title + type)
            audioFile =
                File(dir_path, title + audioType)
            if (file.exists()) {
                file =
                    File(dir_path, title + "_" + dateStr + type)
                audioFile =
                    File(dir_path, title + "_" + dateStr + audioType)
            }
        }

        return file to audioFile
    }

    @JvmStatic
    fun checkRequestPermissions(activity: Activity?, context: Context?): Boolean {
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (permission in appPermissions) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity!!,
                listPermissionsNeeded.toTypedArray(),
                m_ALL_PERMISSIONS_PERMISSION_CODE
            )
            return false
        }
        return true
    }


    object YuYinLog {
        private const val level = Log.INFO
        fun v(tag: String?, msg: String?) {
            if (level <= Log.VERBOSE) {
                Log.v(tag, msg!!)
            }
        }

        fun d(tag: String?, msg: String?) {
            if (level <= Log.DEBUG) {
                Log.v(tag, msg!!)
            }
        }

        fun i(tag: String?, msg: String?) {
            if (level <= Log.INFO) {
                Log.v(tag, msg!!)
            }
        }

        fun w(tag: String?, msg: String?) {
            if (level <= Log.WARN) {
                Log.v(tag, msg!!)
            }
        }

        fun e(tag: String?, msg: String?) {
            if (level <= Log.ERROR) {
                Log.v(tag, msg!!)
            }
        }
    }

    object RecordHelper {
        const val RECORDER_SAMPLERATE = 16000
        const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        var miniBufferSize = 0
        var BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
        var BytesPerElement = 2 // 2 bytes in 16bit format
    }

}


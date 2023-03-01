package com.yuyin.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuyin.demo.view.MainActivityView
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
        var file: File? = null
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

    @JvmStatic
    fun prepareModel(activity: MainActivityView) {
        var model_name = "final"
        var dic_name = "words"
        val sharedPreference =
            PreferenceManager.getDefaultSharedPreferences(activity)
        val mod = sharedPreference.getString("languageOfModule", "zh")
        model_name = "$`model_name`_$mod.zip"
        dic_name = "$`dic_name`_$mod.txt"
        activity.model.model_path = File(activity.assetFilePath(activity, model_name)).absolutePath
        activity.model.dic_path = File(activity.assetFilePath(activity, dic_name)).absolutePath
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

}


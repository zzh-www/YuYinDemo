package com.yuyin.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object YuYinUtil {
    // 所需请求的权限
    val appPermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE
    )
    const val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    @JvmStatic
    fun save_file(context: Context, speechList: List<SpeechText>) {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
        val filename = sdf.format(Date(timeStamp.toString().toLong())) + ".txt"
        val dir_path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file =
            File(dir_path!!.absoluteFile.toString() + File.separator + "YuYin" + File.separator + filename)
        val total_result = StringBuilder()
        for (i in speechList) {
            total_result.append(i.text)
            total_result.append("\n")
        }
        try {
            if (file.createNewFile()) {
                FileOutputStream(file.absolutePath).use { op ->
                    op.write(
                        total_result.toString().toByteArray(
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
        private const val level = Log.VERBOSE
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


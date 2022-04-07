package com.yuyin.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    fun save_file(context: Context, speechList: ArrayList<SpeechText>) {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
        val filename = sdf.format(Date(timeStamp.toString().toLong())) + ".txt"
        val dir_path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file =
            File(dir_path!!.absoluteFile.toString() + File.separator + "YuYin" + File.separator + filename)
        val total_result = StringBuilder()
        if (speechList != null) {
            for (i in speechList) {
                if (i != null) {
                    total_result.append(i.text)
                }
                total_result.append("\n")
            }
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
    fun get_all_result(speechList: ArrayList<SpeechText>) {
//        while (true) {
//            String result = Recognize.getResult();
//            if (Recognize.getFinished()) {
//                break;
//            } else {
//                if (result.endsWith(" ")) {
//                    speechList.get(speechList.size()-1).setText(result.trim());
//                    speechList.add(new SpeechText("..."));
//                } else {
//                    speechList.get(speechList.size()-1).setText(result);
//                }
//            }
//        }
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
}
package com.yuyin.demo.utils

import android.media.AudioFormat
import com.elvishew.xlog.BuildConfig
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.ClassicFlattener
import com.elvishew.xlog.interceptor.BlacklistTagsFilterInterceptor
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.squareup.moshi.Moshi
import com.yuyin.demo.viewmodel.YuyinViewModel
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


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

    const val jsonType = ".json"
    const val audioType = ".wav"
    const val textType = ".txt"

    // view
    private const val LOG_TAG = "YUYIN_RECORD"

    val moshi: Moshi = Moshi.Builder().build()

    const val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    @JvmStatic
    fun getFileName(dir: String, title: String? = null): ResultFiles {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
        val dateStr = sdf.format(Date(timeStamp.toString().toLong()))
        val dir_path = dir
        var name = ""
        var file: File
        var audioFile: File
        var textFile: File
        if (title.isNullOrBlank()) {
            name = dateStr
            file =
                File(dir_path, name + jsonType)
            audioFile =
                File(dir_path, name + audioType)
            textFile =
                File(dir_path, name + textType)
        } else {
            name = title
            file =
                File(dir_path, title + jsonType)
            audioFile =
                File(dir_path, title + audioType)
            textFile =
                File(dir_path, name + textType)
            if (file.exists()) {
                name = title + "_" + dateStr
                file =
                    File(dir_path, name + jsonType)
                audioFile =
                    File(dir_path, name + audioType)
                textFile =
                    File(dir_path, name + textType)
            }
        }

        return ResultFiles(file, audioFile, textFile)
    }

    /***
     * app 统一日志入口
     * 内部使用xLog存储文件
     */
    object YuYinLog {

        init {

            val config = LogConfiguration.Builder()
                .logLevel(
                    if (BuildConfig.DEBUG) LogLevel.ALL // 指定日志级别，低于该级别的日志将不会被打印，默认为 LogLevel.ALL
                    else LogLevel.INFO
                )
                .tag("YuYin") // 指定 TAG，默认为 "X-LOG"
                .enableThreadInfo() // 允许打印线程信息，默认禁止
                .enableStackTrace(2) // 允许打印深度为 2 的调用栈信息，默认禁止
                .enableBorder() // 允许打印日志边框，默认禁止
//                .jsonFormatter(MyJsonFormatter()) // 指定 JSON 格式化器，默认为 DefaultJsonFormatter
//                .xmlFormatter(MyXmlFormatter()) // 指定 XML 格式化器，默认为 DefaultXmlFormatter
//                .throwableFormatter(MyThrowableFormatter()) // 指定可抛出异常格式化器，默认为 DefaultThrowableFormatter
//                .threadFormatter(MyThreadFormatter()) // 指定线程信息格式化器，默认为 DefaultThreadFormatter
//                .stackTraceFormatter(MyStackTraceFormatter()) // 指定调用栈信息格式化器，默认为 DefaultStackTraceFormatter
//                .borderFormatter(MyBoardFormatter()) // 指定边框格式化器，默认为 DefaultBorderFormatter
//                .addObjectFormatter(
//                    AnyClass::class.java,  // 为指定类型添加对象格式化器
//                    AnyClassObjectFormatter()
//                ) // 默认使用 Object.toString()
                .addInterceptor(
                    BlacklistTagsFilterInterceptor( // 添加黑名单 TAG 过滤器
//                        "blacklist1", "blacklist2", "blacklist3"
                    )
                )
//                .addInterceptor(MyInterceptor()) // 添加一个日志拦截器
                .build()

            val androidPrinter: Printer = AndroidPrinter(true) // 通过 android.util.Log 打印日志的打印器

//            val consolePrinter: Printer = ConsolePrinter() // 通过 System.out 打印日志到控制台的打印器

            val filePrinter: Printer =
                FilePrinter.Builder(YuyinViewModel.yuyinLogDir.absolutePath) // 指定保存日志文件的路径
                    .fileNameGenerator(DateFileNameGenerator()) // 指定日志文件名生成器，默认为 ChangelessFileNameGenerator("log")
                    .backupStrategy(NeverBackupStrategy()) // 指定日志文件备份策略，默认为 FileSizeBackupStrategy(1024 * 1024)
                    .cleanStrategy(FileLastModifiedCleanStrategy(24 * 60 * 60 * 30 * 1000L)) // 指定日志文件清除策略，默认为 NeverCleanStrategy()
                    .flattener(ClassicFlattener()) // 指定日志平铺器，默认为 DefaultFlattener
//                .writer(MyWriter()) // 指定日志写入器，默认为 SimpleWriter
                    .build()

            XLog.init( // 初始化 XLog
                config,  // 指定日志配置，如果不指定，会默认使用 new LogConfiguration.Builder().build()
                androidPrinter,  // 添加任意多的打印器。如果没有添加任何打印器，会默认使用 AndroidPrinter(Android)/ConsolePrinter(java)
//                consolePrinter,
                filePrinter
            )

        }

        fun v(tag: String?, msg: String?) {
            XLog.tag(tag).v(msg!!)
        }

        fun d(tag: String?, msg: String?) {
            XLog.tag(tag).d(msg!!)
        }

        fun i(tag: String?, msg: String?) {
            XLog.tag(tag).i(msg!!)
        }

        fun w(tag: String?, msg: String?) {
            XLog.tag(tag).w(msg!!)
        }

        fun e(tag: String?, msg: String?) {
            XLog.tag(tag).e(msg!!)
        }
    }

    fun compressFiles(files: List<File>, zipFile: File) {
        val buffer = ByteArray(1024)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            for (file in files) {
                if (file.exists()) {
                    FileInputStream(file).use { fileIn ->
                        val entry = ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        var len: Int
                        while (fileIn.read(buffer).also { len = it } > 0) {
                            zipOut.write(buffer, 0, len)
                        }
                        zipOut.closeEntry()
                    }
                }
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

    data class ResultFiles(val json: File, val audioFile: File, val textFile: File) {
        companion object {
            /***
             * 获取 txt 和 json路径
             * audio路径为空
             */
            fun getTextAndJson(json: File): ResultFiles {
                val parent = json.parent
                val name = json.nameWithoutExtension
                return ResultFiles(
                    File(parent, name + jsonType),
                    File(""),
                    File(parent, name + textType)
                )
            }
        }
    }
}


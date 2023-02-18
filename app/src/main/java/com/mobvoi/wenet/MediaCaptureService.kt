package com.mobvoi.wenet

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yuyin.demo.MainActivityView
import com.yuyin.demo.R
import com.yuyin.demo.YuYinUtil.ACTION_ALL
import com.yuyin.demo.YuYinUtil.ACTION_START_RECORDING_From_Notification
import com.yuyin.demo.YuYinUtil.ACTION_STOP_RECORDING_From_Notification
import com.yuyin.demo.YuYinUtil.CaptureAudio_ALL
import com.yuyin.demo.YuYinUtil.CaptureAudio_START
import com.yuyin.demo.YuYinUtil.EXTRA_ACTION_NAME
import com.yuyin.demo.YuYinUtil.EXTRA_CaptureAudio_NAME
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.yuyin.demo.YuYinUtil.YuYinLog as Log


class MediaCaptureService : Service() {
    private val LogTag = "MediaCaptureService"
    private val binder: IBinder = MediaServiceBinder()

    private var isCreate = false
    private lateinit var pre_notificationBUilder: NotificationCompat.Builder
    var m_recorder: AudioRecord? = null
    private var m_recorderMic: AudioRecord? = null
    var m_callingIntent: Intent? = null
    private lateinit var m_mediaProjectionManager: MediaProjectionManager
    private lateinit var m_mediaProjection: MediaProjection
    private var pendingIntent: PendingIntent? = null
    private var stopPendingIntent: PendingIntent? = null
    private var startPendingIntent: PendingIntent? = null
    private var m_actionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(ACTION_ALL, ignoreCase = true)) {
                val actionName = intent.getStringExtra(EXTRA_ACTION_NAME)
                if (actionName != null && actionName.isNotEmpty()) {
                    // TODO accept some broadcast
                }
            }
        }
    }

    private fun startMediaProject(intent: Intent) {
        m_mediaProjection = m_mediaProjectionManager.getMediaProjection(-1, intent)
        preStartRecording(m_mediaProjection)
        Log.e("ZZH", "start_recording")
    }

    /**
     * @method startRecording
     * @description 配置采样场景 配置音轨 输出音频样式等
     */
    private fun preStartRecording(mediaProjection: MediaProjection?) {
        // 录制场景
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        // 采样率 编码 掩码
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(m_RECORDER_SAMPLERATE)
            .setEncoding(m_RECORDER_AUDIO_ENCODING)
            .setChannelMask(m_RECORDER_CHANNELS)
            .build()
        miniBufferSize = AudioRecord.getMinBufferSize(
            m_RECORDER_SAMPLERATE,
            m_RECORDER_CHANNELS,
            m_RECORDER_AUDIO_ENCODING
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        m_recorder = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(miniBufferSize)
            .setAudioPlaybackCaptureConfig(config).build()

    }


    override fun onCreate() {
        super.onCreate()
        isCreate = true

        // 中止意图
        val broadStopCastIntent = Intent()
        broadStopCastIntent.action = CaptureAudio_ALL
        broadStopCastIntent.putExtra(
            EXTRA_CaptureAudio_NAME,
            ACTION_STOP_RECORDING_From_Notification
        )
        stopPendingIntent = PendingIntent.getBroadcast(
            this,
            333,
            broadStopCastIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 开始意图
        val broadStartCastIntent = Intent()
        broadStartCastIntent.action = CaptureAudio_ALL
        broadStartCastIntent.putExtra(
            EXTRA_CaptureAudio_NAME,
            ACTION_START_RECORDING_From_Notification
        )
        startPendingIntent = PendingIntent.getBroadcast(
            this,
            334,
            broadStopCastIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 开启通知
        val notificationIntent = Intent(this, MainActivityView::class.java)
        //  Returns an existing or new PendingIntent matching the given parameters
        pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        pre_notificationBUilder = NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
            .setContentTitle("余音")
            .setContentText("ASR")
        m_mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager


        // 注册广播
        val filter = IntentFilter()
        filter.addAction(ACTION_ALL)
        registerReceiver(m_actionReceiver, filter)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId);
        // 启动前台服务
        if (intent==null) {
            Log.e(LogTag,"null start service")
        } else {
            if (m_callingIntent == null)
                m_callingIntent = intent
            val notification = pre_notificationBUilder.build()
            startForeground(m_NOTIFICATION_ID, notification)
            // 配置 recorder
            startMediaProject(m_callingIntent!!)
            // 通知activity 服务已启动
            val broad = Intent()
            broad.action = CaptureAudio_ALL
            broad.putExtra(EXTRA_CaptureAudio_NAME, CaptureAudio_START)
            this.sendBroadcast(broad)
        }
        return START_STICKY //因内存被销毁后， 重新创建
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(m_actionReceiver)
    }

    inner class MediaServiceBinder : Binder() {
        fun serviceRecorder(): AudioRecord? {
            return m_recorder
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**
     * @method getUidExample
     * @description 获取Uid
     * @date: 2021/11/11 2:05
     * @Author: zzh
     */
    fun getUidExample() {
        val pm = packageManager
        val packageInfos = pm.getInstalledPackages(0)
        for (info in packageInfos) {
            Log.i("APPIFO", info.applicationInfo.toString())
        }
    }

    private fun short2byte(sData: ShortArray): ByteArray {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)
        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i].toInt() and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    @SuppressLint("SimpleDateFormat")
    private fun writeAudioDataToFile() {
        if (m_recorder == null) return
        // Write the output audio in byte
        Log.i("ZZH", "Recording started. Computing output file name")
        val sampleDir =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "/TestRecordingDasa1")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }
        val fileName = "Record-" + SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(Date()) + ".pcm"
        val filePath = sampleDir.absolutePath + "/" + fileName
        //String filePath = "/sdcard/voice8K16bitmono.pcm";
        val sData = ShortArray(BufferElements2Rec)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (m_isRecording) {
            // gets the voice output from microphone to byte format
            m_recorder?.read(sData, 0, BufferElements2Rec)
            Log.i("ZZH", "Short wirting to file$sData")
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                val bData = short2byte(sData)
                os!!.write(bData, 0, BufferElements2Rec * BytesPerElement)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i("ZZH", "record error:" + e.message)
            }
        }
        try {
            os!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath))
    }

    @SuppressLint("SimpleDateFormat")
    private fun writeAudioDataToFileMic() {
        if (m_recorderMic == null) return
        // Write the output audio in byte
        Log.i("ZZH", "Recording started. Computing output file name")
        val sampleDir =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "/TestRecordingDasa1Mic")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }
        val fileName = "Record-" + SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(Date()) + ".pcm"
        val filePath = sampleDir.absolutePath + "/" + fileName
        //String filePath = "/sdcard/voice8K16bitmono.pcm";
        val sData = ShortArray(BufferElements2Rec)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        while (m_isRecording) {
            // gets the voice output from microphone to byte format
            m_recorderMic!!.read(sData, 0, BufferElements2Rec)
            Log.i("ZZH", "Short wirting to file$sData")
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                val bData = short2byte(sData)
                os!!.write(bData, 0, BufferElements2Rec * BytesPerElement)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i("ZZH", "record error:" + e.message)
            }
        }
        try {
            os!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath))
    }

    companion object {
        const val m_RECORDER_SAMPLERATE = 16000
        const val m_RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        const val m_RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val MAX_QUEUE_SIZE = 2500
        var miniBufferSize = 0
        private var m_isRecording = false
        const val m_NOTIFICATION_CHANNEL_ID = "Yuyin_ChannelId"
        const val m_NOTIFICATION_CHANNEL_NAME = "Yuyin_Channel"
        const val m_NOTIFICATION_CHANNEL_DESC = "Yuyin is working"
        const val m_NOTIFICATION_ID = 1000
        var BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
        var BytesPerElement = 2 // 2 bytes in 16bit format
    }
}
package com.mobvoi.wenet

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.os.Process
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yuyin.demo.MainActivityView
import com.yuyin.demo.R
import com.yuyin.demo.RuningCapture
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.experimental.and

class MediaCaptureService : Service() {
    val bufferQueue: BlockingQueue<ShortArray> = ArrayBlockingQueue(MAX_QUEUE_SIZE)
    private val m_Log_TAG = "MediaCaptureService"
    private val binder: IBinder = mcs_Binder()
    private val m_NOTIFICATION_ID = 1000
    var BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    var BytesPerElement = 2 // 2 bytes in 16bit format
    var m_notificationManager: NotificationManager? = null
    lateinit var m_recorder: AudioRecord
    var m_recorderMic: AudioRecord? = null
    var m_callingIntent: Intent? = null
    private var isCreate = false
    private var m_notificationBuilder: NotificationCompat.Builder? = null
    private var m_mediaProjectionManager: MediaProjectionManager? = null
    private var m_mediaProjection: MediaProjection? = null
    private var pendingIntent: PendingIntent? = null
    private var stopPendingIntent: PendingIntent? = null
    private var startPendingIntent: PendingIntent? = null
    var m_actionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(RuningCapture.ACTION_ALL, ignoreCase = true)) {
                val actionName = intent.getStringExtra(RuningCapture.EXTRA_ACTION_NAME)
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equals(RuningCapture.ACTION_START, ignoreCase = true)) {
                        // 接受通知启动录制
                        try {
                            startMediaProject(m_callingIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (actionName.equals(
                            RuningCapture.ACTION_START_RECORDING,
                            ignoreCase = true
                        )
                    ) {
//                        stopRecording(m_callingIntent);
                        restartRecording()
                    } else if (actionName.equals(
                            RuningCapture.ACTION_STOP_RECORDING,
                            ignoreCase = true
                        )
                    ) {
                        stopRecording()
                    } else if (actionName.equals(RuningCapture.ACTION_STOP, ignoreCase = true)) {
                        releaseRecording()
                    } else if (actionName.equals(
                            RuningCapture.ACTION_STOP_RECORDING_To_Main,
                            ignoreCase = true
                        )
                    ) {
                        stopRecordingToMain()
                    }
                }
            }
        }
    }

    private fun startMediaProject(intent: Intent?) {
        m_mediaProjection = m_mediaProjectionManager!!.getMediaProjection(-1, intent!!)
        startRecording(m_mediaProjection)
        Log.e("ZZH", "start_recording")
    }

    /**
     * @method startRecording
     * @description 配置采样场景 配置音轨 输出音频样式等
     */
    private fun startRecording(mediaProjection: MediaProjection?) {
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
        m_isRecording = true
        Thread {
            val broad = Intent()
            broad.action = RuningCapture.CaptureAudio_ALL
            broad.putExtra(
                RuningCapture.EXTRA_CaptureAudio_NAME,
                RuningCapture.CaptureAudio_START_ASR
            )
            this.sendBroadcast(broad)
        }.start()
        Thread {
            m_recorder.startRecording()
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (m_isRecording) {
                val buffer = ShortArray(miniBufferSize / 2)
                val read = m_recorder.read(buffer, 0, buffer.size)
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        bufferQueue.put(buffer)
                    }
                } catch (e: InterruptedException) {
                    Log.e(m_Log_TAG, e.message!!)
                }
            }
        }.start()


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                writeAudioDataToFile();
//            }
//        }).start();
    }

    private fun restartRecording() {
        m_isRecording = true
        m_notificationBuilder = NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
            .setContentTitle("余音")
            .setContentText("ASR working")
            .addAction(R.drawable.ic_baseline_play_arrow_24, "stop", stopPendingIntent)
        val notification = m_notificationBuilder!!.build()
        m_notificationManager!!.notify(m_NOTIFICATION_ID, notification)
        Thread {
            val broad = Intent()
            broad.action = RuningCapture.CaptureAudio_ALL
            broad.putExtra(
                RuningCapture.EXTRA_CaptureAudio_NAME,
                RuningCapture.CaptureAudio_RESTART_RECORDING
            )
            this.sendBroadcast(broad)
        }.start()
        Thread {
            m_recorder.startRecording()
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (m_isRecording) {
                val buffer = ShortArray(miniBufferSize / 2)
                val read = m_recorder.read(buffer, 0, buffer.size)
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        bufferQueue.put(buffer)
                    }
                } catch (e: InterruptedException) {
                    Log.e(m_Log_TAG, e.message!!)
                }
            }
        }.start()
    }

    private fun stopRecording() {
        m_isRecording = false
        m_recorder.stop()
        m_notificationBuilder = NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
            .setContentTitle("余音")
            .setContentText("ASR stop")
            .addAction(R.drawable.ic_baseline_play_arrow_24, "start", startPendingIntent)
        val notification = m_notificationBuilder!!.build()
        m_notificationManager!!.notify(m_NOTIFICATION_ID, notification)

//            m_mediaProjection.stop();
        Thread {
            val broad = Intent()
            broad.action = RuningCapture.CaptureAudio_ALL
            broad.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.CaptureAudio_STOP)
            this.sendBroadcast(broad)
        }.start()
    }

    private fun stopRecordingToMain() {
        m_isRecording = false
        m_recorder.stop()
    }

    private fun releaseRecording() {

        m_isRecording = false
        m_recorder.stop()
        m_recorder.release()
        m_mediaProjection!!.stop()
        stopForeground(true)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        isCreate = true
        val broadStopCastIntent = Intent()
        broadStopCastIntent.action = RuningCapture.CaptureAudio_ALL
        broadStopCastIntent.putExtra(
            RuningCapture.EXTRA_CaptureAudio_NAME,
            RuningCapture.ACTION_STOP_RECORDING_From_Notification
        )
        stopPendingIntent = PendingIntent.getBroadcast(
            this,
            333,
            broadStopCastIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val broadStartCastIntent = Intent()
        broadStartCastIntent.action = RuningCapture.CaptureAudio_ALL
        broadStartCastIntent.putExtra(
            RuningCapture.EXTRA_CaptureAudio_NAME,
            RuningCapture.ACTION_START_RECORDING_From_Notification
        )
        startPendingIntent = PendingIntent.getBroadcast(
            this,
            334,
            broadStopCastIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val notificationIntent = Intent(this, MainActivityView::class.java)
        //  Returns an existing or new PendingIntent matching the given parameters
        pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        m_notificationBuilder = NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
            .setContentTitle("余音")
            .setContentText("ASR working")
            .setTicker(m_ONGING_NOTIFICATION_TICKER) //通知到来时低版本上会在系统状态栏显示一小段时间 5.0以上版本好像没有用了
            .addAction(R.drawable.ic_baseline_stop_24, "stop", stopPendingIntent)
        val channel = NotificationChannel(
            m_NOTIFICATION_CHANNEL_ID,
            m_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = m_NOTIFICATION_CHANNEL_DESC
        m_notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        m_notificationManager!!.createNotificationChannel(channel)
        m_mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val filter = IntentFilter()
        filter.addAction(RuningCapture.ACTION_ALL)
        registerReceiver(m_actionReceiver, filter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId);
        // 启动
        m_callingIntent = intent
        val notification = m_notificationBuilder!!.build()
        startForeground(m_NOTIFICATION_ID, notification)

        // 通知服务已启动
        Thread {
            val broad = Intent()
            broad.action = RuningCapture.CaptureAudio_ALL
            broad.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.CaptureAudio_START)
            this.sendBroadcast(broad)
        }.start()
        return START_STICKY //因内存被销毁后， 重新创建
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(m_actionReceiver)
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
            bytes[i * 2] = sData[i].and(0x00FF).toByte()
            bytes[i * 2 + 1] = sData[i].toInt().shr(8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    private fun writeAudioDataToFile() {
        if (m_recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) return
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
            m_recorder.read(sData, 0, BufferElements2Rec)
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

    inner class mcs_Binder : Binder() {
        @get:Throws(InterruptedException::class)
        val audioQueue: ShortArray
            get() = bufferQueue.take()

        @Throws(InterruptedException::class)
        fun getAudioQueueSize(): Int {
            return bufferQueue.size
        }

        fun clearQueue() {
            bufferQueue.clear()
        }

        fun getIsCreate(): Boolean {
            return isCreate
        }
    }

    companion object {
        const val m_NOTIFICATION_CHANNEL_ID = "Yuyin_ChannelId"
        const val m_NOTIFICATION_CHANNEL_NAME = "Yuyin_Channel"
        const val m_NOTIFICATION_CHANNEL_DESC = "Yuyin is working"
        private const val m_RECORDER_SAMPLERATE = 16000
        private const val m_RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private const val m_RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val m_ONGING_NOTIFICATION_TICKER = "YuyinRecorderApp"
        private const val MAX_QUEUE_SIZE = 2500
        private var miniBufferSize = 0
        private var m_isRecording = false
    }
}
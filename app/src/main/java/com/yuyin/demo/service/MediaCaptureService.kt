package com.yuyin.demo.service

import android.Manifest
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
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yuyin.demo.R
import com.yuyin.demo.utils.YuYinUtil.ACTION_ALL
import com.yuyin.demo.utils.YuYinUtil.ACTION_START_RECORDING_From_Notification
import com.yuyin.demo.utils.YuYinUtil.ACTION_STOP_RECORDING_From_Notification
import com.yuyin.demo.utils.YuYinUtil.CaptureAudio_ALL
import com.yuyin.demo.utils.YuYinUtil.CaptureAudio_START
import com.yuyin.demo.utils.YuYinUtil.EXTRA_ACTION_NAME
import com.yuyin.demo.utils.YuYinUtil.EXTRA_CaptureAudio_NAME
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_AUDIO_ENCODING
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_CHANNELS
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_SAMPLERATE
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.miniBufferSize
import com.yuyin.demo.view.MainActivityView
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log


class MediaCaptureService : Service() {
    private val TAG = "MediaCaptureService"
    private val binder: IBinder = MediaServiceBinder()

    private var isCreate = false
    private lateinit var pre_notificationBUilder: NotificationCompat.Builder
    var m_recorder: AudioRecord? = null
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
        Log.e(TAG, "start_recording")
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
            .setSampleRate(RECORDER_SAMPLERATE)
            .setEncoding(RECORDER_AUDIO_ENCODING)
            .setChannelMask(RECORDER_CHANNELS)
            .build()
        miniBufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING
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
            .setSmallIcon(R.drawable.cyuyin)
            .setColor(theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary)).getColor(0,ContextCompat.getColor(this,R.color.md_theme_light_primary)))
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
            Log.e(TAG,"null start service")
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
            Log.d("APPIFO", info.applicationInfo.toString())
        }
    }
    


    companion object {
        const val m_NOTIFICATION_CHANNEL_ID = "Yuyin_ChannelId"
        const val m_NOTIFICATION_CHANNEL_NAME = "Yuyin_Channel"
        const val m_NOTIFICATION_CHANNEL_DESC = "Yuyin is working"
        const val m_NOTIFICATION_ID = 1000
    }
}
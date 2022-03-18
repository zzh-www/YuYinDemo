package com.mobvoi.wenet;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.yuyin.demo.MainActivityView;
import com.yuyin.demo.R;
import com.yuyin.demo.RuningCapture;
import com.yuyin.demo.YuYinLog;

public class MediaCaptureService extends Service {

    private static final int m_RECORDER_SAMPLERATE = 16000;
    private static final int m_RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int m_RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final String m_ONGING_NOTIFICATION_TICKER = "YuyinRecorderApp";
    private static final int MAX_QUEUE_SIZE = 2500;
    private static int miniBufferSize;
    private static boolean m_isRecording = false;
    private final String m_Log_TAG = "MediaCaptureService";
    public static final String m_NOTIFICATION_CHANNEL_ID = "Yuyin_ChannelId";
    public static final String m_NOTIFICATION_CHANNEL_NAME = "Yuyin_Channel";
    public static final String m_NOTIFICATION_CHANNEL_DESC = "Yuyin is working";

    private final IBinder binder = new mcs_Binder();
    public final BlockingQueue<short[]> bufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private boolean isCreate = false;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private NotificationCompat.Builder m_notificationBuilder;
    private NotificationCompat.Builder pre_notificationBUilder;
    NotificationManager m_notificationManager;
    AudioRecord m_recorder;
    AudioRecord m_recorderMic;
    Intent m_callingIntent;
    private final int m_NOTIFICATION_ID = 1000;
    private MediaProjectionManager m_mediaProjectionManager;
    private MediaProjection m_mediaProjection;

    private PendingIntent pendingIntent;
    private PendingIntent stopPendingIntent;
    private PendingIntent startPendingIntent;


    BroadcastReceiver m_actionReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(RuningCapture.ACTION_ALL)) {
                String actionName = intent.getStringExtra(RuningCapture.EXTRA_ACTION_NAME);
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equalsIgnoreCase(RuningCapture.ACTION_START)) {
                        // 接受通知启动录制
                        try {
                            startMediaProject(m_callingIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (actionName.equalsIgnoreCase(RuningCapture.ACTION_START_RECORDING)) {
                        startRecording();
                        changeYourUIToStop();
                    } else if (actionName.equalsIgnoreCase(RuningCapture.ACTION_STOP_RECORDING)) {
                        stopRecording();
                        changeYourUIToStart();
                    } else if (actionName.equalsIgnoreCase(RuningCapture.ACTION_STOP)) {
                        releaseRecording();
                    } else if (actionName.equalsIgnoreCase(RuningCapture.ACTION_STOP_NOW)) {
                        stopRecording();
                    }
                }
            }
        }
    };


    private void startMediaProject(Intent intent) {
        m_mediaProjection = m_mediaProjectionManager.getMediaProjection(-1, intent);
        prestartRecording(m_mediaProjection);
        YuYinLog.e("ZZH", "start_recording");
    }





    /**
     * @method startRecording
     * @description 配置采样场景 配置音轨 输出音频样式等
     */
    private void prestartRecording(MediaProjection mediaProjection) {
        // 录制场景
        AudioPlaybackCaptureConfiguration config =
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

        // 采样率 编码 掩码
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setSampleRate(m_RECORDER_SAMPLERATE)
                .setEncoding(m_RECORDER_AUDIO_ENCODING)
                .setChannelMask(m_RECORDER_CHANNELS)
                .build();

        miniBufferSize = AudioRecord.getMinBufferSize(m_RECORDER_SAMPLERATE, m_RECORDER_CHANNELS, m_RECORDER_AUDIO_ENCODING);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        m_recorder = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(miniBufferSize)
                .setAudioPlaybackCaptureConfig(config).build();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                writeAudioDataToFile();
//            }
//        }).start();
    }

    private void startRecording() {
        m_isRecording = true;
        m_notificationBuilder = new NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
                .setContentTitle("余音")
                .setContentText("ASR working")
                .addAction(R.drawable.ic_baseline_play_arrow_24, "stop", stopPendingIntent);
        Notification notification = m_notificationBuilder.build();
        m_notificationManager.notify(m_NOTIFICATION_ID,notification);
        new Thread(() -> {
            m_recorder.startRecording();
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (m_isRecording) {
                short[] buffer = new short[miniBufferSize / 2];
                int read = m_recorder.read(buffer, 0, buffer.length);
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        bufferQueue.put(buffer);
                    }
                } catch (InterruptedException e) {
                    YuYinLog.e(m_Log_TAG, e.getMessage());
                }
            }
        }).start();
    }

    private void changeYourUIToStop(){
        Intent broad = new Intent();
        broad.setAction(RuningCapture.CaptureAudio_ALL);
        broad.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.CaptureAudio_START_ASR);
        this.sendBroadcast(broad);

    }

    private void stopRecording() {

        m_isRecording = false;

        m_recorder.stop();
        m_notificationBuilder = new NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
                .setContentTitle("余音")
                .setContentText("ASR stop")
                .addAction(R.drawable.ic_baseline_play_arrow_24, "start", startPendingIntent);
        Notification notification = m_notificationBuilder.build();
        m_notificationManager.notify(m_NOTIFICATION_ID,notification);

    }
    private void changeYourUIToStart(){
        Intent broad = new Intent();
        broad.setAction(RuningCapture.CaptureAudio_ALL);
        broad.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.CaptureAudio_STOP);
        this.sendBroadcast(broad);
    }

    private void releaseRecording() {
        if (m_recorder != null) {
            m_isRecording = false;
            m_recorder.stop();
            m_recorder.release();
            m_recorder = null;
        }

        m_mediaProjection.stop();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isCreate = true;

        // 中至意图
        Intent broadStopCastIntent = new Intent();
        broadStopCastIntent.setAction(RuningCapture.CaptureAudio_ALL);
        broadStopCastIntent.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.ACTION_STOP_RECORDING_From_Notification);
        stopPendingIntent = PendingIntent.getBroadcast(this, 333, broadStopCastIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // 开始意图
        Intent broadStartCastIntent = new Intent();
        broadStartCastIntent.setAction(RuningCapture.CaptureAudio_ALL);
        broadStartCastIntent.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.ACTION_START_RECORDING_From_Notification);
        startPendingIntent = PendingIntent.getBroadcast(this, 334, broadStopCastIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // 开启通知
        Intent notificationIntent = new Intent(this, MainActivityView.class);
        //  Returns an existing or new PendingIntent matching the given parameters
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        pre_notificationBUilder = new NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(ContextCompat.getColor(this, R.color.primaryDarkColor))
                .setContentTitle("余音")
                .setContentText("ASR");
        NotificationChannel channel = new NotificationChannel(m_NOTIFICATION_CHANNEL_ID, m_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(m_NOTIFICATION_CHANNEL_DESC);
        m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m_notificationManager.createNotificationChannel(channel);

        m_mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);


        // 注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(RuningCapture.ACTION_ALL);
        registerReceiver(m_actionReceiver, filter);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        // 启动
        m_callingIntent = intent;
        Notification notification = pre_notificationBUilder.build();
        startForeground(m_NOTIFICATION_ID, notification);

        // 通知服务已启动
        new Thread(() -> {
            Intent broad = new Intent();
            broad.setAction("MainActivityAction");
            broad.putExtra(RuningCapture.EXTRA_CaptureAudio_NAME, RuningCapture.CaptureAudio_START);
            this.sendBroadcast(broad);
        }).start();
        return START_STICKY; //因内存被销毁后， 重新创建

    }


    @Override
    public void onDestroy() {

        super.onDestroy();
        unregisterReceiver(m_actionReceiver);
    }

    public class mcs_Binder extends Binder {
        public short[] getAudioQueue() throws InterruptedException {
            return bufferQueue.take();
        }
        public int getAudioQueueSize() {
            return bufferQueue.size();
        }

        public void clearQueue() {
            bufferQueue.clear();
        }

        public boolean getIsCreate() {
            return isCreate;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * @method getUidExample
     * @description 获取Uid
     * @date: 2021/11/11 2:05
     * @Author: zzh
     */
    public void getUidExample() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
        for (PackageInfo info : packageInfos) {
            YuYinLog.i("APPIFO", info.applicationInfo.toString());
        }
    }


    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        if (m_recorder == null) return;
        // Write the output audio in byte
        YuYinLog.i("ZZH", "Recording started. Computing output file name");
        File sampleDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "/TestRecordingDasa1");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String fileName = "Record-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".pcm";
        String filePath = sampleDir.getAbsolutePath() + "/" + fileName;
        //String filePath = "/sdcard/voice8K16bitmono.pcm";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (m_isRecording) {
            // gets the voice output from microphone to byte format
            m_recorder.read(sData, 0, BufferElements2Rec);
            YuYinLog.i("ZZH", "Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
                YuYinLog.i("ZZH", "record error:" + e.getMessage());
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        YuYinLog.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath));
    }

    private void writeAudioDataToFileMic() {
        if (m_recorderMic == null) return;
        // Write the output audio in byte
        YuYinLog.i("ZZH", "Recording started. Computing output file name");
        File sampleDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "/TestRecordingDasa1Mic");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String fileName = "Record-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".pcm";
        String filePath = sampleDir.getAbsolutePath() + "/" + fileName;
        //String filePath = "/sdcard/voice8K16bitmono.pcm";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (m_isRecording) {
            // gets the voice output from microphone to byte format
            m_recorderMic.read(sData, 0, BufferElements2Rec);
            YuYinLog.i("ZZH", "Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
                YuYinLog.i("ZZH", "record error:" + e.getMessage());
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        YuYinLog.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath));
    }
}

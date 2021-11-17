package com.mobvoi.wenet;


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
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MediaCaptureService extends Service {

    public static final String ACTION_ALL = "ALL";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_ACTION_NAME = "ACTION_NAME";
    private static final int m_RECORDER_SAMPLERATE = 16000;
    private static final int m_RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int m_RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final String m_ONGING_NOTIFICATION_TICKER = "YuyinRecorderApp";
    private static final int MAX_QUEUE_SIZE = 2500;
    private static int miniBufferSize;
    private static boolean m_isRecording = false;
    private final String m_Log_TAG = "MediaCaptureService";
    private final String m_NOTIFICATION_CHANNEL_ID = "Yuyin_ChannelId";
    private final String m_NOTIFICATION_CHANNEL_NAME = "Yuyin_Channel";
    private final String m_NOTIFICATION_CHANNEL_DESC = "Yuyin is working";
    private final BlockingQueue<String> resultText = new ArrayBlockingQueue<>(100);
    private final IBinder binder = new mcs_Binder();
    public final BlockingQueue<short[]> bufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private boolean isCreate = false;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    NotificationCompat.Builder m_notificationBuilder;
    NotificationManager m_notificationManager;
    AudioRecord m_recorder;
    AudioRecord m_recorderMic;
    Intent m_callingIntent;
    private final int m_NOTIFICATION_ID = 1000;
    private MediaProjectionManager m_mediaProjectionManager;
    private MediaProjection m_mediaProjection;


//    private AudioRecognize m_audioRecognize;


    BroadcastReceiver m_actionReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(ACTION_ALL)) {
                String actionName = intent.getStringExtra(EXTRA_ACTION_NAME);
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equalsIgnoreCase(ACTION_START)) {
                        startRecording(m_callingIntent);
                    } else {
                        stopRecording(m_callingIntent);
                    }
                }
            }
        }
    };


    /**
     * @param intent Intent
     * @return void
     * @method startRecording
     * @description 启动录音服务
     * @date: 2021/11/10 22:34
     * @Author: zzh
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startRecording(Intent intent) {
        m_mediaProjection = m_mediaProjectionManager.getMediaProjection(-1, intent);
        startRecording(m_mediaProjection);
        Log.e("ZZH","start_recording");
//        m_audioRecognize.initASR();
//        startASR();
//        m_audioRecognize.AsrDecode();
    }


    /**
     * @method startRecording
     * @description 配置采样场景 配置音轨 输出音频样式等
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startRecording(MediaProjection mediaProjection) {
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

        m_recorder = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(miniBufferSize)
                .setAudioPlaybackCaptureConfig(config).build();
        m_isRecording = true;

        new Thread(()->{
            Intent broad = new Intent();
            broad.setAction(CaptureAudio.CaptureAudio_ALL);
            broad.putExtra(CaptureAudio.EXTRA_CaptureAudio_NAME,CaptureAudio.CaptureAudio_START_ASR);
            this.sendBroadcast(broad);
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        Log.e(m_Log_TAG, e.getMessage());
                    }
                }
            }
        }).start();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                writeAudioDataToFile();
//            }
//        }).start();
    }

    private void stopRecording(Intent intent) {
        if (m_recorder != null) {
            m_isRecording = false;
            m_recorder.stop();
            m_recorder.release();
            m_recorder = null;
        }
        m_mediaProjection.stop();
        stopSelf(); // 停止本服务
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 启动
            Intent notificationIntent = new Intent(this, MediaCaptureService.class);
            //  Returns an existing or new PendingIntent matching the given parameters
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            m_notificationBuilder = new NotificationCompat.Builder(this, m_NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("余音")
                    .setContentText("ASR starting")
                    .setTicker(m_ONGING_NOTIFICATION_TICKER) //通知到来时低版本上会在系统状态栏显示一小段时间 5.0以上版本好像没有用了
                    .setContentIntent(pendingIntent);
            Notification notification = m_notificationBuilder.build();
            NotificationChannel channel = new NotificationChannel(m_NOTIFICATION_CHANNEL_ID, m_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(m_NOTIFICATION_CHANNEL_DESC);
            m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            m_notificationManager.createNotificationChannel(channel);
            startForeground(m_NOTIFICATION_ID, notification);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            m_mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ALL);
        registerReceiver(m_actionReceiver, filter);

    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);

        isCreate = true;
        m_callingIntent = intent;
        new Thread(()->{
            Intent broad = new Intent();
            broad.setAction(CaptureAudio.CaptureAudio_ALL);
            broad.putExtra(CaptureAudio.EXTRA_CaptureAudio_NAME, CaptureAudio.CaptureAudio_START);
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
        short[] getAudioQueue() throws InterruptedException {
            short[] data = bufferQueue.take();
            return data;
        }

        int getAudioQueueSize() {
            return bufferQueue.size();
        }

        boolean getIsRecording(){
            return m_isRecording;
        }

        boolean getIsCreate() {
            return isCreate;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * @return void
     * @method getUidExample
     * @description 获取Uid
     * @date: 2021/11/11 2:05
     * @Author: zzh
     */
    public void getUidExample() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
        for (PackageInfo info : packageInfos) {
            Log.i("APPIFO", info.applicationInfo.toString());
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
        if(m_recorder==null) return;
        // Write the output audio in byte
        Log.i("ZZH", "Recording started. Computing output file name");
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
            Log.i("ZZH", "Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("ZZH", "record error:" + e.getMessage());
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath));
    }

    private void writeAudioDataToFileMic() {
        if(m_recorderMic==null) return;
        // Write the output audio in byte
        Log.i("ZZH", "Recording started. Computing output file name");
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
            Log.i("ZZH", "Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("ZZH", "record error:" + e.getMessage());
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("ZZH", String.format("Recording finished. File saved to '%s'", filePath));
    }
}

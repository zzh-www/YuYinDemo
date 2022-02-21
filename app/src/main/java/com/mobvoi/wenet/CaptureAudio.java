package com.mobvoi.wenet;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.yuyin.demo.R;

@RequiresApi(api = Build.VERSION_CODES.P)
public class CaptureAudio extends AppCompatActivity {
    private final String Tag = "ZZH";

    public static final String CaptureAudio_ALL = "CaptureAudio";
    public static final String CaptureAudio_START = "CaptureAudio_START";
    public static final String CaptureAudio_START_ASR = "CaptureAudio_START_ASR";
    public static final String CaptureAudio_STOP = "CaptureAudio_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_CaptureAudio_NAME = "CaptureAudio_NAME";
    private boolean m_startRecording = false;
    private static final int m_ALL_PERMISSIONS_PERMISSION_CODE = 1000;
    private static final int m_CREATE_SCREEN_CAPTURE = 1001;
    private MediaProjectionManager m_mediaProjectionManager;
    private boolean is_init =  false;
    private CaptureAudioReceiver m_actionReceiver;
    Button luyin;
    MediaCaptureService mediaCaptureService;
    boolean mBound = false;
    private MediaCaptureService.mcs_Binder mcs_binder;

    // 所需请求的权限
    private final String[] appPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        stopRecording();
    }

    // 不可以耗时操作  在主线程中
    class CaptureAudioReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(CaptureAudio_ALL)) {
                String actionName = intent.getStringExtra(EXTRA_CaptureAudio_NAME);
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equalsIgnoreCase(CaptureAudio_START)) {
                        startRecording();
                    }
                    else if (actionName.equalsIgnoreCase(CaptureAudio_START_ASR)) {
                            startAsrThread();
                            Recognize.startDecode();
                    }
                }
            }
        }
    }



    // 每次开始录音时都应该调用 确保具有权限
    public boolean checkRequestPermissions() {
        List< String > listPermissionsNeeded = new ArrayList<>();
        for(String permission : appPermissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(permission);
            }
        }

        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), m_ALL_PERMISSIONS_PERMISSION_CODE);
            return false;
        }

        return true;
    }

    /**
     * @Author zzh
     * @Description 回调 检测权限有没有成功申请
     * @Date 17:56 2021/11/10
     * @Param [requestCode, permissions, grantResults]
     * @return void
     **/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == m_ALL_PERMISSIONS_PERMISSION_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;
            for (int permissionIndx = 0; permissionIndx < permissions.length; permissionIndx++) {
                if (grantResults[permissionIndx] != PackageManager.PERMISSION_GRANTED) {
                    permissionResults.put(permissions[permissionIndx], grantResults[permissionIndx]);
                    deniedCount++;
                }
            }

            if (deniedCount != 0)
                Log.e(Tag, "Permission Denied!  Now you must allow  permission from settings.");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_audio);
        luyin  = findViewById(R.id.recording);
        new Thread(()->{
            IntentFilter filter = new IntentFilter();
            filter.addAction(CaptureAudio_ALL);
            m_actionReceiver = new CaptureAudioReceiver();
            registerReceiver(m_actionReceiver, filter);
        }).start();

        checkRequestPermissions();

        new Thread(()->{
            Intent i = new Intent(this, MediaCaptureService.class);
            bindService(i,connection, Context.BIND_AUTO_CREATE);
        }).start();

        TextView m_text = findViewById(R.id.textView1);

        luyin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!m_startRecording) {
                    // 检测权限
                    if (checkRequestPermissions()) {
                        Log.e(Tag,"权限获取成功");
                        System.out.println("权限获取成功");
                        initAudioCapture();
                        runOnUiThread(()->luyin.setEnabled(false));
                        runOnUiThread(()-> luyin.setText("stop"));
                        Recognize.reset();
                    } else {
                        Log.e(Tag,"权限");
                    }
                }
                else {
                    stopRecording();
                    Recognize.setInputFinished();
                }
            }
        });
    }

    /**
     * @Author zzh
     * @Description 弹出 屏幕录制请求窗口 回调onActivityResult() 成功 则启动服务
     * @Date 17:55 2021/11/10
     * @Param []
     * @return void
     **/
    @TargetApi(Build.VERSION_CODES.P)
    private void initAudioCapture() {
        m_mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = m_mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, m_CREATE_SCREEN_CAPTURE); // 会在service保存此intent 获取前线
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (m_CREATE_SCREEN_CAPTURE == requestCode) {
            Intent i = new Intent(this, MediaCaptureService.class);
            i.setAction(MediaCaptureService.ACTION_ALL);
            i.putExtra(MediaCaptureService.EXTRA_RESULT_CODE,resultCode);
            i.putExtras(data);
            this.startService(i);
        } else {
            Toast.makeText(this, "must allow", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        // 广播告知 MediaCaptureService
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(MediaCaptureService.ACTION_ALL);
        // 具体action
        // 根据 MediaCaptureService.EXTRA_ACTION_NAME("ACTION_NAME") 对应的值
        broadCastIntent.putExtra(MediaCaptureService.EXTRA_ACTION_NAME, MediaCaptureService.ACTION_START);
        this.sendBroadcast(broadCastIntent);
        m_startRecording = true;

    }

    private void stopRecording() {
        runOnUiThread(()-> {
            luyin.setText("start");
            m_startRecording = false;
        });
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(MediaCaptureService.ACTION_ALL);
        broadCastIntent.putExtra(MediaCaptureService.EXTRA_ACTION_NAME, MediaCaptureService.ACTION_STOP);
        this.sendBroadcast(broadCastIntent);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mcs_binder = (MediaCaptureService.mcs_Binder) service;
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    /**
     * @method  startAsrThread
     * @description  //TODO 似乎有断词 未全转的情况  需要  测试一下 麦克风  和 录制音频 （同一段音频区别）
     * @date: 2021/11/15 15:32
     * @Author: zzh
     */
    private void startAsrThread() {
        luyin.setClickable(true);
        runOnUiThread(()->luyin.setEnabled(true));
        new Thread(() -> {
            // Send all data
            while (m_startRecording || mcs_binder.getAudioQueueSize() > 0) {
                try {
                    short[] data = mcs_binder.getAudioQueue();
                    // 1. add data to C++ interface
                    Recognize.acceptWaveform(data);
                    // 2. get partial result
                    runOnUiThread(() -> {
                        TextView textView = findViewById(R.id.textView1);
                        textView.setText(Recognize.getResult());
                    });
                } catch (InterruptedException e) {
                    Log.e("ZZH", e.getMessage());
                }
            }

            // Wait for final result
            while (true) {
                // get result
                if (!Recognize.getFinished()) {
                    runOnUiThread(() -> {
                        Recognize.reset();
                        TextView textView = findViewById(R.id.textView1);
                        textView.setText(Recognize.getResult());
                    });
                } else {
                    runOnUiThread(() -> {
                        Button button = findViewById(R.id.recording);
                        button.setEnabled(true);
                    });
                    break;
                }
            }
        }).start();
    }
}


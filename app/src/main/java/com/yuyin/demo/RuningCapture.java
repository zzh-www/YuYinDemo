package com.yuyin.demo;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;

import android.media.projection.MediaProjectionManager;

import android.os.Bundle;


import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lzf.easyfloat.EasyFloat;
import com.mobvoi.wenet.MediaCaptureService;
import com.mobvoi.wenet.Recognize;
import com.yuyin.demo.databinding.FragmentRuningCaptureBinding;


import java.util.ArrayList;


public class RuningCapture extends Fragment {

    public static final String CaptureAudio_ALL = "CaptureAudio";
    public static final String CaptureAudio_START = "CaptureAudio_START";
    public static final String CaptureAudio_RESTART_RECORDING = "CaptureAudio_RESTART_RECORDING";
    public static final String CaptureAudio_START_ASR = "CaptureAudio_START_ASR";
    public static final String CaptureAudio_STOP = "CaptureAudio_STOP";

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_CaptureAudio_NAME = "CaptureAudio_NAME";
    public static final int m_CREATE_SCREEN_CAPTURE = 1001;
    public static final String EXTRA_ACTION_NAME = "ACTION_NAME";
    public static final String ACTION_ALL = "ALL";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_START_RECORDING = "CaptureAudio_START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "CaptureAudio_STOP_RECORDING";
    public static final String ACTION_STOP_RECORDING_From_Notification = "ACTION_STOP_RECORDING_From_Notification";
    public static final String ACTION_STOP_RECORDING_To_Main = "CaptureAudio_STOP_RECORDING_To_Main";
    public static final String ACTION_START_RECORDING_From_Notification = "CaptureAudio_START_RECORDING_From_Notification";


    // view
    private static final String LOG_TAG = "YUYIN_RECORD";
    private FragmentRuningCaptureBinding binding;


    private ArrayList<SpeechText> speechList;
    private SpeechTextAdapter adapter;
    private RecyclerView recyclerView;

    // ViewModel
    private YuyinViewModel model;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentRuningCaptureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initRunner();

        binding.stopBtRunCap.setOnClickListener(v -> {
            //TODO stopASR
            if (model.getStartRecord()) {
                getActivity().runOnUiThread(() -> {
                    binding.stopBtRunCap.setEnabled(false);
                    stopRecording();
                    binding.stopBtRunCap.setText("start");
                    binding.saveBtRunCap.setVisibility(View.VISIBLE);
                    binding.saveBtRunCap.setEnabled(true);
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    binding.stopBtRunCap.setEnabled(false);
                    restartRecording();
                    binding.stopBtRunCap.setText("stop");
                    binding.saveBtRunCap.setVisibility(View.INVISIBLE);
                    binding.saveBtRunCap.setEnabled(false);
                });

            }

        });

        binding.saveBtRunCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // get all Resukt
                YuYinUtil.get_all_result(speechList);
                // saveToFile
                YuYinUtil.save_file(model.getContext(), speechList);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        model.setChange_senor(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().runOnUiThread(() -> {
            model.setStartRecord(false);
            model.setStartAsr(false);
            binding = null;
        });
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        model = new ViewModelProvider(requireActivity()).get(YuyinViewModel.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().runOnUiThread(() -> {
            stopRecordingToActivity();
            model.setStartAsr(false);
            Recognize.setInputFinished();
        });
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    // init view
    private void initRunner() {


        // 滚动视图
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(model.getContext());
        speechList = new ArrayList<SpeechText>();
        recyclerView = binding.recyclerRunCap;
        recyclerView.setLayoutManager(linearLayoutManager);

        if (model.getResultsSize() == null || model.getResultsSize() < 1) {
            speechList.add(new SpeechText("Hi"));
//            speechList = model.getResults().getValue();
        } else {
            speechList = model.getResults().getValue();
        }

        adapter = new SpeechTextAdapter(speechList);
        recyclerView.setAdapter(adapter);

        if (model.getChange_senor()) {
            if (model.getStartRecord()) {
                //TODO start service is always start
                initAudioCapture();
                binding.stopBtRunCap.setText("stop");
                binding.saveBtRunCap.setVisibility(View.INVISIBLE);
                binding.saveBtRunCap.setEnabled(false);
            } else {
                binding.stopBtRunCap.setText("start");
                binding.saveBtRunCap.setVisibility(View.VISIBLE);
                binding.saveBtRunCap.setEnabled(true);
            }
        } else {


            model.setStartAsr(false);


            if (model.getMBound()) {
                // restart
                Recognize.reset();
                model.setStartAsr(true);
                restartRecording();
                startAsrThread();
                Recognize.startDecode();
            } else {
                initAudioCapture(); // start service
            }


            model.getContext().runOnUiThread(() -> {
                binding.stopBtRunCap.setText("stop");
                binding.saveBtRunCap.setVisibility(View.INVISIBLE);
                binding.saveBtRunCap.setEnabled(false);
            });


        }
    }


    // 广播服务

    // 不可以耗时操作  在主线程中
    public class CaptureAudioReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(CaptureAudio_ALL)) {
                String actionName = intent.getStringExtra(EXTRA_CaptureAudio_NAME);
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equalsIgnoreCase(CaptureAudio_START)) {

                        // 接收服务已启动的通知
                        // 通知服务开启录制
                        startRecordingService();
                    } else if (actionName.equalsIgnoreCase(CaptureAudio_START_ASR)) {
                        // binding 变为了null？？？
                        model.getContext().runOnUiThread(() -> {
                            model.getContext().findViewById(R.id.stop_bt_run_cap).setEnabled(true);
                            model.setStartRecord(true);
                        });
                        // 接收录制已启动通知 只有第一次需要
                        if (!model.getStartAsr()) {
                            Recognize.reset();
                            startAsrThread();
                            Recognize.startDecode();
                            model.setStartAsr(true);
                        }
                    } else if (actionName.equalsIgnoreCase(CaptureAudio_STOP)) {
                        model.getContext().runOnUiThread(() -> {
                            model.setStartRecord(false);
                            TextView floatText = EasyFloat.getFloatView("Capture").findViewById(R.id.flow_text);
                            floatText.setText("");
                            // 跳出当前fragment后
                            try {
                                model.getContext().findViewById(R.id.stop_bt_run_cap).setEnabled(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else if (actionName.equalsIgnoreCase(CaptureAudio_RESTART_RECORDING)) {
                        model.getContext().runOnUiThread(() -> {
                            model.getContext().findViewById(R.id.stop_bt_run_cap).setEnabled(true);
                            model.setStartRecord(true);
                        });

                        if (model.getStartAsr()==false) {
                            model.setStartAsr(true);
                            startAsrThread();
                        }
                    } else if (actionName.equalsIgnoreCase(ACTION_STOP_RECORDING_From_Notification)) {
                        model.context.runOnUiThread(()->{
                            binding.stopBtRunCap.setEnabled(false);
                            stopRecording();
                            binding.stopBtRunCap.setText("start");
                            binding.saveBtRunCap.setVisibility(View.VISIBLE);
                            binding.saveBtRunCap.setEnabled(true);
                        });
                    } else if (actionName.equalsIgnoreCase(ACTION_START_RECORDING_From_Notification)) {
                        model.context.runOnUiThread(() -> {
                            binding.stopBtRunCap.setEnabled(false);
                            restartRecording();
                            binding.stopBtRunCap.setText("stop");
                            binding.saveBtRunCap.setVisibility(View.INVISIBLE);
                            binding.saveBtRunCap.setEnabled(false);
                        });
                    }
                }
            }
        }
    }


    private void startRecordingService() {

        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(ACTION_ALL);
            broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START);
            model.getContext().sendBroadcast(broadCastIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartRecording() {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START_RECORDING);
        model.getContext().sendBroadcast(broadCastIntent);
    }

    private void stopRecording() {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP_RECORDING);
        model.getContext().sendBroadcast(broadCastIntent);
    }

    private void stopRecordingToActivity() {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP_RECORDING_To_Main);
        model.getContext().sendBroadcast(broadCastIntent);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            model.setMcs_binder((MediaCaptureService.mcs_Binder) service);
            model.setMBound(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            model.setMBound(false);
        }
    };


    private void initAudioCapture() {

        // 未启动服务
        if (!model.getMBound()) {
            // Service
            MediaProjectionManager m_mediaProjectionManager = (MediaProjectionManager) model.getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent intent = m_mediaProjectionManager.createScreenCaptureIntent();
            // 获取录制屏幕权限 并启动服务
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            new Thread(() -> {
                                IntentFilter filter = new IntentFilter();
                                filter.addAction(CaptureAudio_ALL);
                                model.m_actionReceiver = new CaptureAudioReceiver();
                                model.getContext().registerReceiver(model.m_actionReceiver, filter);

                                Intent i = new Intent(model.getContext(), MediaCaptureService.class);
                                model.getContext().bindService(i, connection, Context.BIND_AUTO_CREATE);
                            }).start();
                            // 启动服务
                            Intent i = new Intent(model.getContext(), MediaCaptureService.class);
                            i.setAction(ACTION_ALL);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra(EXTRA_RESULT_CODE, m_CREATE_SCREEN_CAPTURE);
                            i.putExtras(result.getData());
                            model.getContext().startService(i);
                        } else {
                            Navigation.findNavController(model.getContext(), R.id.yuyin_nav_host_container_fragment).popBackStack();
                        }
                    }
            ).launch(intent);
        }
    }

    // Asr
    private void startAsrThread() {

        new Thread(() -> {
            while (model.getStartAsr()) {
                try {
                    if (binding == null) break;
                    short[] data = model.getMcs_binder().getAudioQueue();
                    if (data != null) {
                        // 1. add data to C++ interface
                        Recognize.acceptWaveform(data);// 将音频传到模型
                    }

                    // 2. get partial result

                    String result = Recognize.getResult();

                    if (Recognize.getResult()=="") {
                        model.context.runOnUiThread(()->{
                            TextView floatText = EasyFloat.getFloatView("Capture").findViewById(R.id.flow_text);
                            floatText.setText(result);
                        });
                    }

                    if (result.endsWith(" ")) {
                        model.context.runOnUiThread(() -> {
                            speechList.get(speechList.size() - 1).setText(result.trim());
                            adapter.notifyItemChanged(speechList.size() - 1);
                            speechList.add(new SpeechText("..."));
                            adapter.notifyItemInserted(speechList.size() - 1);
                            recyclerView.scrollToPosition(speechList.size() - 1);
                        });
                    } else {
                        model.context.runOnUiThread(() -> {
                            speechList.get(speechList.size() - 1).setText(result);
                            adapter.notifyItemChanged(speechList.size() - 1);
//                            recyclerView.scrollToPosition(speechList.size()-1);
                            TextView floatText = EasyFloat.getFloatView("Capture").findViewById(R.id.flow_text);
                            floatText.setText(result);
                        });
                        // 部分结果
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    Log.e(LOG_TAG + "GETWRONG", "runonui");
                    e.printStackTrace();
                }
            }


        }).start();

        model.setStartAsr(false);
    }

}


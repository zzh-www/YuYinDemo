package com.yuyin.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lzf.easyfloat.EasyFloat;
import com.mobvoi.wenet.MediaCaptureService;
import com.mobvoi.wenet.Recognize;
import com.yuyin.demo.databinding.FragmentRuningCaptureBinding;

import java.util.ArrayList;


public class RuningCapture extends Fragment {

    public static final String CaptureAudio_ALL = "CaptureAudio";
    public static final String CaptureAudio_START = "CaptureAudio_START";
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
    public static final String ACTION_STOP_NOW = "CaptureAudio_STOP_RECORDING_without_ui";
    public static final String ACTION_STOP_RECORDING_From_Notification = "ACTION_STOP_RECORDING_From_Notification";
    public static final String ACTION_START_RECORDING_From_Notification = "CaptureAudio_START_RECORDING_From_Notification";


    // view
    private static final String LOG_TAG = "YUYIN_RECORD";
    private FragmentRuningCaptureBinding binding;


    private ArrayList<SpeechText> speechList;
    private SpeechTextAdapter adapter;
    private RecyclerView recyclerView;

    // ViewModel
    private YuyinViewModel model;

    // activityContext
    public Button stop_button;
    public Button save_button;
    public TextView floatText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentRuningCaptureBinding.inflate(inflater, container, false);
        stop_button = requireActivity().findViewById(R.id.stop_bt_run_cap);
        save_button = requireActivity().findViewById(R.id.save_bt_run_cap);
        floatText = EasyFloat.getFloatView("Capture").findViewById(R.id.flow_text);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initRunner();

        binding.stopBtRunCap.setOnClickListener(v -> {
            if (model.getStartRecord()) {
                requireActivity().runOnUiThread(() -> {
                    binding.stopBtRunCap.setEnabled(false);
                    stopRecording(requireActivity());
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    binding.stopBtRunCap.setEnabled(false);
                    startRecording(requireActivity());
                });

            }

        });

        binding.saveBtRunCap.setOnClickListener(v -> {

            // get all Resukt
            YuYinUtil.get_all_result(speechList);
            // saveToFile
            YuYinUtil.save_file(requireActivity(), speechList);
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
        requireActivity().runOnUiThread(() -> {
            binding = null;
            model.setStartAsr(false);
            model.setStartRecord(false);
            Intent i = new Intent();
            i.setAction(ACTION_ALL);
            i.putExtra(EXTRA_ACTION_NAME,ACTION_STOP_NOW);
            requireActivity().sendBroadcast(i);
        });

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        model = new ViewModelProvider(requireActivity()).get(YuyinViewModel.class);
    }

    @Override
    public void onDestroy() {
//        activity = null;
        super.onDestroy();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    // init view
    private void initRunner() {


        // 滚动视图
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        speechList = new ArrayList<>();
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

    }




    public static void startRecordingService(Activity activity) {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START);
        activity.sendBroadcast(broadCastIntent);
    }



    public static void startRecording(Activity activity) {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START_RECORDING);
        activity.sendBroadcast(broadCastIntent);
    }



    public static void stopRecording(Activity activity) {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP_RECORDING);
        activity.sendBroadcast(broadCastIntent);
    }

    public static void stopRecordingToActivity(Activity activity) {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_ALL);
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP);
        activity.sendBroadcast(broadCastIntent);
    }

//    private final ServiceConnection connection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            model.setMcs_binder((MediaCaptureService.mcs_Binder) service);
//            model.setMBound(true);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            model.setMBound(false);
//        }
//    };


    @Override
    public void onResume() {
        super.onResume();
    }

//    private void initAudioCapture() {
//
//        // 未启动服务
//        // Service
//        MediaProjectionManager m_mediaProjectionManager = (MediaProjectionManager) requireActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        Intent intent = m_mediaProjectionManager.createScreenCaptureIntent();
//        // 获取录制屏幕权限 并启动服务
//        registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        new Thread(() -> {
//
//                            Intent i = new Intent(requireActivity(), MediaCaptureService.class);
//                            requireActivity().bindService(i, connection, Context.BIND_AUTO_CREATE);
//                        }).start();
//                        // 启动服务
//                        Intent i = new Intent(requireActivity(), MediaCaptureService.class);
//                        i.setAction(ACTION_ALL);
//                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        i.putExtra(EXTRA_RESULT_CODE, m_CREATE_SCREEN_CAPTURE);
//                        i.putExtras(result.getData());
//                        requireActivity().startService(i);
//
//                    } else {
//                        Navigation.findNavController(requireActivity(), R.id.yuyin_nav_host_container_fragment).popBackStack();
//                    }
//                }
//        ).launch(intent);
//    }

    // Asr
    public void startAsrThread() {
        new Thread(() -> {
            Recognize.reset();
            Recognize.startDecode();
            while (true) {
                if (model.getStartAsr()){
//                    Log.w(LOG_TAG,"get");
                    try {
                        if (getActivity() == null) break;
                        short[] data = model.getMcs_binder().getAudioQueue();
                        // 1. add data to C++ interface
                        Recognize.acceptWaveform(data);// 将音频传到模型

                        // 2. get partial result

                        String result = Recognize.getResult();

                        if (result.endsWith(" ")) {
                            requireActivity().runOnUiThread(() -> {
                                speechList.get(speechList.size() - 1).setText(result.trim());
                                adapter.notifyItemChanged(speechList.size() - 1);
                                speechList.add(new SpeechText("..."));
                                adapter.notifyItemInserted(speechList.size() - 1);
                                recyclerView.scrollToPosition(speechList.size() - 1);
                            });
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                speechList.get(speechList.size() - 1).setText(result);
                                adapter.notifyItemChanged(speechList.size() - 1);
                                floatText.setText(result);
                            });
                            // 部分结果
                        }

                    } catch (IllegalStateException e) {
                        Log.e(LOG_TAG,e.toString());
                        break;
                    } catch (InterruptedException e) {
                        Log.e(LOG_TAG,e.toString());
                    }
                } else {
                    break;
                }

            }


        }).start();
    }

    public void waitForFinished() {
        Recognize.setInputFinished();
        new Thread(()->{
            while (!model.getStartAsr() ||model.getMcs_binder().getAudioQueueSize()>0){
                if (Recognize.getFinished()){
                    requireActivity().runOnUiThread(()->{
                        floatText.setText("");
                        updateUiToStart();
                    });
                    break;
                }
                String result = Recognize.getResult();


                if (result.endsWith(" ")) {
                    requireActivity().runOnUiThread(() -> {
                        speechList.get(speechList.size() - 1).setText(result.trim());
                        adapter.notifyItemChanged(speechList.size() - 1);
                        speechList.add(new SpeechText("..."));
                        adapter.notifyItemInserted(speechList.size() - 1);
                        recyclerView.scrollToPosition(speechList.size() - 1);
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        speechList.get(speechList.size() - 1).setText(result);
                        adapter.notifyItemChanged(speechList.size() - 1);
                        floatText.setText(result);
                    });
                }
            }
        }).start();
    }

    public void updateUiToStop() {
        requireActivity().runOnUiThread(()->{
            binding.stopBtRunCap.setText("stop");
            binding.stopBtRunCap.setEnabled(true);
            binding.saveBtRunCap.setVisibility(View.INVISIBLE);
            binding.saveBtRunCap.setEnabled(false);
        });
    }

    public void updateUiToStart() {
        requireActivity().runOnUiThread(()->{
            binding.stopBtRunCap.setText("start");
            binding.stopBtRunCap.setEnabled(true);
            binding.saveBtRunCap.setVisibility(View.VISIBLE);
            binding.saveBtRunCap.setEnabled(true);
        });
    }
}


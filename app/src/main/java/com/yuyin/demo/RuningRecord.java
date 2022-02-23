package com.yuyin.demo;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobvoi.wenet.Recognize;

import com.yuyin.demo.databinding.FragmentRuningRecordBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;


public class RuningRecord extends Fragment {

    private FragmentRuningRecordBinding binding;
    private final String LOG_TAG = "YUYIN_RECORD";

    // record
    private static final int SAMPLE_RATE = 16000;  // The sampling rate
    private AudioRecord record = null;

    private int miniBufferSize = 0;


    // 滚动视图
    private ArrayList<SpeechText> speechList = new ArrayList<SpeechText>();
    private SpeechTextAdapter adapter;
    private RecyclerView recyclerView;

    // ViewModel
    private YuyinViewModel model;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentRuningRecordBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        return view;
//        return inflater.inflate(R.layout.fragment_runing_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        model = new ViewModelProvider(requireActivity()).get(YuyinViewModel.class);

        // 滚动视图
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView = binding.recyclerRunRecord;
        recyclerView.setLayoutManager(linearLayoutManager);
        if (model.getResultsSize()==null || model.getResultsSize()<1) {
            speechList.add(new SpeechText("Hi"));
        } else {
            speechList = model.getResults().getValue();
        }
        adapter = new SpeechTextAdapter(speechList);
        recyclerView.setAdapter(adapter);
        // false false
        // true true

        if (model.getChange_senor()) {
            // 屏幕旋转 而重构

            // 旋转前正在录制 应该继续录制
            if (model.getStartRecord()) {
                initRecorder();
                startRecordThread();
                binding.stopBtRunRecord.setText("stop");
                binding.saveBtRunRecord.setVisibility(View.INVISIBLE);
                binding.saveBtRunRecord.setEnabled(false);
            } else {
                binding.stopBtRunRecord.setText("start");
                binding.saveBtRunRecord.setVisibility(View.VISIBLE);
                binding.saveBtRunRecord.setEnabled(true);
            }
        } else {
            // 正常启动绘制
                initRecorder();
                startRecordThread();
                binding.stopBtRunRecord.setText("stop");
                binding.saveBtRunRecord.setVisibility(View.INVISIBLE);
                binding.saveBtRunRecord.setEnabled(false);
                Recognize.reset();
                Recognize.startDecode();
                startAsrThread();
        }



        binding.stopBtRunRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //只需停止录音即可
                if (model.getStartRecord()) {
                    getActivity().runOnUiThread(()->{
                        binding.stopBtRunRecord.setEnabled(false);
                        model.setStartRecord(false);
                        binding.stopBtRunRecord.setText("start");
                        binding.saveBtRunRecord.setVisibility(View.VISIBLE);
                        binding.saveBtRunRecord.setEnabled(true);
                    });
                } else {
                    initRecorder();
                    startRecordThread();
                    model.setStartRecord(true);
                    binding.saveBtRunRecord.setVisibility(View.INVISIBLE);
                    binding.saveBtRunRecord.setEnabled(false);
                    // Recognize.startDecode();
                    // startAsrThread();
                }
            }
        });

        binding.saveBtRunRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO saveResult
                Long timeStamp = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
                String filename = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))))+".txt";
                File dir_path = getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                File file = new File(dir_path.getAbsoluteFile()+File.separator+"YuYin"+File.separator+filename);
                StringBuilder total_result = new StringBuilder();

                for (SpeechText i : speechList ) {
                    total_result.append(i.getText());
                    total_result.append("\n");
                }
                try {
                    if (file.createNewFile()) {
                        try (OutputStream op = new FileOutputStream(file.getAbsolutePath())) {
                            op.write(total_result.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        model.setChange_senor(true); // 标记屏幕旋转
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().runOnUiThread(()->{
            model.setStartRecord(false);
            model.setStartAsr(false);
            binding = null;
            Recognize.reset();
        });

        model.getResults().setValue(speechList);
    }
    @Override
    public void onDestroy() {
        model.getResults().getValue().clear();
        model.getBufferQueue().clear();
        super.onDestroy();
    }

    private void initRecorder() {
//    // buffer size in bytes 1280
        miniBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG_TAG, "Audio buffer can't initialize!");
            return;
        }


        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                miniBufferSize);
        Log.i(LOG_TAG, "Record init okay");

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
        }


    }

    private void startRecordThread() {
//        model.setStartRecord(true);
        new Thread(() -> {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
            record.startRecording();
            getActivity().runOnUiThread(()->{
                binding.stopBtRunRecord.setText("stop");
            });
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (model.getStartRecord()) {
                short[] buffer = new short[miniBufferSize / 2];
                int read = record.read(buffer, 0, buffer.length);
//        voiceView.add(calculateDb(buffer));
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
//                        bufferQueue.put(buffer)
                        model.getBufferQueue().put(buffer);
                    }
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }

            }
            record.stop();

            if (binding != null) {
                getActivity().runOnUiThread(()->{
                    if (binding != null)
                        binding.stopBtRunRecord.setEnabled(true); });
            }
//      voiceView.zero();
        }).start();
    }

    private void startAsrThread() {
        new Thread(() -> {
            // Send all data
            model.setStartAsr(true);
            while (model.getStartAsr() || model.getBufferQueue().size() > 0) {
                try {
                    if(binding==null) break;
                    short[] data = model.getBufferQueue().take();
                    // 1. add data to C++ interface
                    Recognize.acceptWaveform(data);// 将音频传到模型

                    // 2. get partial result

                    String result = Recognize.getResult();
                    if (result.equals("")) continue;
                    if (result.endsWith(" ")) {
                        getActivity().runOnUiThread(()->{
                            speechList.get(speechList.size()-1).setText(result.trim());
                            adapter.notifyItemChanged(speechList.size()-1);
                            speechList.add(new SpeechText("..."));
                            adapter.notifyItemInserted(speechList.size()-1);
                            recyclerView.scrollToPosition(speechList.size()-1);
                        });
                    } else {
                        getActivity().runOnUiThread(()->{
                            speechList.get(speechList.size()-1).setText(result);
                            adapter.notifyItemChanged(speechList.size()-1);
//                            recyclerView.scrollToPosition(speechList.size()-1);
                        });
                        // 部分结果
                    }



                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    Log.e(LOG_TAG+"GETWRONG","runonui");
                }
            }

//            // Wait for final result
//            while (model.getStartAsr()) {
//                // get result
//                String result = Recognize.getResult();
//                if (!Recognize.getFinished()) {
//                    if (result.endsWith(" ")) {
//                        model.getResults().getValue().get(model.getResultsSize()-1).setText(result.trim());
//                        model.getResults().getValue().add(new SpeechText("..."));
//                    } else {
//                        try {
//                            model.getResults().getValue().get(model.getResultsSize()-1).setText(result.trim());
//                        }
//                    }
//                } else {
//                    break;
//                }
//            }
        }).start();
    }
}
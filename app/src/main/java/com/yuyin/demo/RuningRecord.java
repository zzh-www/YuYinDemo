package com.yuyin.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobvoi.wenet.Recognize;

import com.yuyin.demo.databinding.FragmentRuningRecordBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class RuningRecord extends Fragment {

    private FragmentRuningRecordBinding binding;
    private final String LOG_TAG = "YUYIN_RECORD";

    // record
    private static final int SAMPLE_RATE = 16000;  // The sampling rate
    private static final int MAX_QUEUE_SIZE = 2500;  // 100 seconds audio, 1 / 0.04 * 100
    private AudioRecord record = null;
    private boolean startRecord = false;
    private int miniBufferSize = 0;
    private final BlockingQueue<short[]> bufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    // 滚动视图
    private List<SpeechText> speechList = new ArrayList<SpeechText>();
    private SpeechTextAdapter adapter;
    private RecyclerView recyclerView;
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

        // 滚动视图
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView = binding.recyclerRunRecord;
        recyclerView.setLayoutManager(linearLayoutManager);
        speechList.add(new SpeechText("Hi"));
        adapter = new SpeechTextAdapter(speechList);
        recyclerView.setAdapter(adapter);


        initRecorder();

        Recognize.reset();

        startRecordThread();

        startRecord = true;

        Recognize.startDecode();

        startAsrThread();



        binding.stopBtRunRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO stopASR
                if (startRecord) {
                    getActivity().runOnUiThread(()->{
                        startRecord = false;
                    });
                } else {
                    initRecorder();
                    startRecordThread();
                    startRecord = true;
                    Recognize.startDecode();
                    startAsrThread();
                }
            }
        });

        binding.saveBtRunRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO saveResult
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
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
        new Thread(() -> {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
            record.startRecording();
            getActivity().runOnUiThread(()->{
                    binding.stopBtRunRecord.setText("stop");
            });
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (startRecord) {
                short[] buffer = new short[miniBufferSize / 2];
                int read = record.read(buffer, 0, buffer.length);
//        voiceView.add(calculateDb(buffer));
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        bufferQueue.put(buffer);
                    }
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
//        Button button = findViewById(R.id.button);
//        if (!button.isEnabled() && startRecord) {
//          runOnUiThread(() -> button.setEnabled(true));
//        }
            }
            record.stop();
//      voiceView.zero();
        }).start();
    }

    private void startAsrThread() {
        new Thread(() -> {
            int start = 0;
            StringBuilder pre_string = new StringBuilder();
            // Send all data
            while (startRecord || bufferQueue.size() > 0) {
                try {

                    short[] data = bufferQueue.take();
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

            // Wait for final result
            while (true) {
                // get result
                String result = Recognize.getResult();
                getActivity().runOnUiThread(()->{
                    binding.stopBtRunRecord.setEnabled(false);
                });
                if (result.equals("")) continue;
                if (!Recognize.getFinished()) {
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
                            // 部分结果
                            speechList.get(speechList.size()-1).setText(result);
                            adapter.notifyItemChanged(speechList.size()-1);
                        });
                    }
                } else {
                    getActivity().runOnUiThread(() -> {
                        binding.stopBtRunRecord.setText("start");
                        binding.stopBtRunRecord.setEnabled(true);
                    });
                    break;
                }
            }
        }).start();
    }
}
package com.mobvoi.wenet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends AppCompatActivity {

  private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
  private static final String LOG_TAG = "WENET";
  private static final int SAMPLE_RATE = 16000;  // The sampling rate
  private static final int MAX_QUEUE_SIZE = 2500;  // 100 seconds audio, 1 / 0.04 * 100
  private MediaProjectionManager m_mediaProjectionManager;
  private MediaProjection m_mediaProjection;
  private boolean startRecord = false;
  private AudioRecord record = null;
  AudioRecord m_record = null;
  private int miniBufferSize = 0;  // 1280 bytes 648 byte 40ms, 0.04s
  private final BlockingQueue<short[]> bufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
  private static final int m_CREATE_SCREEN_CAPTURE = 1001;
  private final String[] appPermissions = {
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.FOREGROUND_SERVICE
  };

  private List<SpeechText> speechList = new ArrayList<SpeechText>();
  private SpeechTextAdapter adapter;
  private RecyclerView recyclerView;

  public static String assetFilePath(Context context, String assetName) {
    File file = new File(context.getFilesDir(), assetName);
    if (file.exists() && file.length() > 0) {
      return file.getAbsolutePath();
    }

    try (InputStream is = context.getAssets().open(assetName)) {
      try (OutputStream os = new FileOutputStream(file)) {
        byte[] buffer = new byte[4 * 1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
          os.write(buffer, 0, read);
        }
        os.flush();
      }
      return file.getAbsolutePath();
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error process asset " + assetName + " to file path");
    }
    return null;
  }


  @RequiresApi(api = Build.VERSION_CODES.Q)
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(LOG_TAG, "record permission is granted");
        initRecoder();
      } else {
        Toast.makeText(this, "Permissions denied to record audio", Toast.LENGTH_LONG).show();
        Button button = findViewById(R.id.button);
        button.setEnabled(false);
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button Test = findViewById(R.id.button3);
    Test.setOnClickListener(v -> {
      Intent cap = new Intent(this, CaptureAudio.class);
      startActivity(cap);
    });

    Button floatView = findViewById(R.id.button4);
    floatView.setOnClickListener(v->{
      Intent flo = new Intent(this, TestFloat.class);
      startActivity(flo);
    });

    Button button_init = findViewById(R.id.button2);
    button_init.setOnClickListener(v -> {
      requestAudioPermissions();
//      final String modelPath = new File(assetFilePath(this, "final.zip")).getAbsolutePath();
//      final String dictPath = new File(assetFilePath(this, "words.txt")).getAbsolutePath();
//      Recognize.init(modelPath, dictPath);
    });

    new Thread(() -> {
      final String modelPath = new File(assetFilePath(this, "final.zip")).getAbsolutePath();
      final String dictPath = new File(assetFilePath(this, "words.txt")).getAbsolutePath();
      Recognize.init(modelPath, dictPath);
    }).start();

    // 滚动视图
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    recyclerView = findViewById(R.id.recycler);
    recyclerView.setLayoutManager(linearLayoutManager);
    speechList.add(new SpeechText("Hi!"));
    adapter = new SpeechTextAdapter(speechList);
    recyclerView.setAdapter(adapter);


    Button button = findViewById(R.id.button);
    button.setText("Start Record");
    button.setOnClickListener(view -> {
      if (!startRecord) {
        startRecord = true;
        Recognize.reset();
        startRecordThread();
        startAsrThread();
        Recognize.startDecode();
        button.setText("Stop Record");
      } else {
        startRecord = false;
        Recognize.setInputFinished();
        button.setText("Start Record");
      }
      button.setEnabled(false);
    });
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  private void requestAudioPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.RECORD_AUDIO},
              MY_PERMISSIONS_RECORD_AUDIO);
    } else {
      initRecoder();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    m_mediaProjection = m_mediaProjectionManager.getMediaProjection(resultCode, data);
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  private void initRecoder() {
//    // buffer size in bytes 1280
    miniBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
      Log.e(LOG_TAG, "Audio buffer can't initialize!");
      return;
    }


    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
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
      return;
    }


  }


  private void startRecordThread() {
    new Thread(() -> {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
      record.startRecording();
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
        Button button = findViewById(R.id.button);
        if (!button.isEnabled() && startRecord) {
          runOnUiThread(() -> button.setEnabled(true));
        }
      }
      record.stop();
//      voiceView.zero();
    }).start();
  }

  private double calculateDb(short[] buffer) {
    double energy = 0.0;
    for (short value : buffer) {
      energy += value * value;
    }
    energy /= buffer.length;
    energy = (10 * Math.log10(1 + energy)) / 100;
    energy = Math.min(energy, 1.0);
    return energy;
  }

  private void startAsrThread() {
    new Thread(() -> {
      // Send all data
      while (startRecord || bufferQueue.size() > 0) {
        try {
          short[] data = bufferQueue.take();
          // 1. add data to C++ interface
          Recognize.acceptWaveform(data);
          // 2. get partial result
          runOnUiThread(() -> {
            speechList.add(new SpeechText(Recognize.getResult()));
            adapter.notifyItemInserted(speechList.size()-1);
            recyclerView.scrollToPosition(speechList.size()-1);
          });
        } catch (InterruptedException e) {
          Log.e(LOG_TAG, e.getMessage());
        }
      }

      // Wait for final result
      while (true) {
        // get result
        if (!Recognize.getFinished()) {
          runOnUiThread(() -> {
            speechList.get(speechList.size()-1).setText(Recognize.getResult());
            adapter.notifyItemChanged(speechList.size()-1);
          });
        } else {
          runOnUiThread(() -> {
            Button button = findViewById(R.id.button);
            button.setEnabled(true);
          });
          break;
        }
      }
    }).start();
  }
}
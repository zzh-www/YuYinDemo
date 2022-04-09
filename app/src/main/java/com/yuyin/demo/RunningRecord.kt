package com.yuyin.demo

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import com.yuyin.demo.YuYinUtil.YuYinLog as Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningRecordBinding
import com.yuyin.demo.models.RunningRecordViewModel
import com.yuyin.demo.models.YuyinViewModel
import kotlinx.coroutines.*
import java.lang.Exception


class RunningRecord : Fragment() {
    private val LOG_TAG = "YUYIN_RECORD"
    private var _binding: FragmentRuningRecordBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    private val flowView = EasyFloat.getFloatView("Capture")!!
        .findViewById<TextView>(R.id.flow_text)

    // ViewModel
    private val model: RunningRecordViewModel by viewModels()

    private val yuYinModel: YuyinViewModel by activityViewModels()

    private var initModel = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRuningRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRunner()
        // init model
        model.viewModelScope.launch(Dispatchers.IO) {
            YuYinUtil.prepareModel(requireActivity() as MainActivityView)
            Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)  // 初始化模型
            initModel = true
            withContext(Dispatchers.Main){
                // 订阅结果
                model.updateFlow(flowView, recyclerView)
            }
        }
        binding.stopBtRunRecord.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.IO) {
                    while (!initModel) {

                    }
                    withContext(Dispatchers.Main) {
                        binding.stopBtRunRecord.isEnabled = true
                        model.recordState = false
                        model.asrState = false
                        flowView.text = ""
                    }
                    model.record.stop()
                    if (!Recognize.getFinished())
                        Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.stopBtRunRecord.text = "start"
                        binding.saveBtRunRecord.visibility = View.VISIBLE
                        binding.saveBtRunRecord.isEnabled = true
                    }
                }
            } else {
                model.viewModelScope.launch(Dispatchers.IO) {
                    while (!initModel) {

                    }
                    Recognize.reset()
                    startRecord()
                    withContext(Dispatchers.Main) {
                        model.recordState = true
                        model.asrState = true
                        binding.saveBtRunRecord.visibility = View.INVISIBLE
                        binding.saveBtRunRecord.isEnabled = false
                    }
                    Recognize.startDecode()
                    model.getTextFlow()
                }
            }
        }
        binding.saveBtRunRecord.setOnClickListener { // get all Result
            // saveToFile
            save_file(requireContext(), model.speechList)
        }


    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        model.change_senor = false // 标记屏幕旋转
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        model.asrState = false
        model.recordState = false
        model.record.release()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initRunner() {
        // 滚动视图
        model.linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding.recyclerRunRecord
        recyclerView.layoutManager = model.linearLayoutManager
        recyclerView.adapter = model.adapter
        // false false
        // true true
        if (!model.change_senor) {
            // 正常启动绘制
            initRecorder()
            binding.stopBtRunRecord.text = "start"
            binding.saveBtRunRecord.visibility = View.VISIBLE
            binding.saveBtRunRecord.isEnabled = true
        }
    }

    private fun initRecorder() {
        // buffer size in bytes 1280
        model.miniBufferSize = AudioRecord.getMinBufferSize(
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (model.miniBufferSize == AudioRecord.ERROR || model.miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG_TAG, "Audio buffer can't initialize!")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        model.record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            model.miniBufferSize
        )
        Log.i(LOG_TAG, "Record init okay")
        if (model.record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!")
        }
    }

    private fun startRecord() {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
        model.record.startRecording()
        binding.stopBtRunRecord.text = "stop"
        model.produceAudio()
    }


}
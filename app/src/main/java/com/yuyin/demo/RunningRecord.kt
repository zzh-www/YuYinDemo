package com.yuyin.demo

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningRecordBinding
import com.yuyin.demo.models.RunningRecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch


class RunningRecord : Fragment() {
    private val LOG_TAG = "YUYIN_RECORD"
    private var _binding: FragmentRuningRecordBinding? = null
    private val binding get() = _binding!!
    private var miniBufferSize = 0

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    private val flowView = EasyFloat.getFloatView("Capture")!!
        .findViewById<TextView>(R.id.flow_text)

    // ViewModel
    private val model: RunningRecordViewModel by viewModels()

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
        binding.stopBtRunRecord.setOnClickListener {
            //只需停止录音即可
            if (model.recordState) {
                binding.stopBtRunRecord.isEnabled = false
                model.recordState = false
                model.asrState = false
                binding.stopBtRunRecord.text = "start"
                binding.saveBtRunRecord.visibility = View.VISIBLE
                binding.saveBtRunRecord.isEnabled = true
            } else {
                initRecorder()
                startRecordThread()
                model.recordState = true
                model.asrState = true
                binding.saveBtRunRecord.visibility = View.INVISIBLE
                binding.saveBtRunRecord.isEnabled = false
//                Recognize.startDecode();
//                startAsrThread();
                model.getTextFlow()
                model.updateFlow(flowView, recyclerView)
            }
        }
        binding.saveBtRunRecord.setOnClickListener { // get all Result
            // saveToFile
            save_file(requireContext(), model.speechList)
        }


    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        model.change_senor = true // 标记屏幕旋转
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Recognize.setInputFinished()
        super.onDestroy()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun initRunner() {

        // 滚动视图
        model.linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding.recyclerRunRecord
        recyclerView.layoutManager = model.linearLayoutManager
        recyclerView.adapter = model.adapter
        // false false
        // true true
        if (model.change_senor) {
            // 屏幕旋转 而重构

            // 旋转前正在录制 应该继续录制
            if (model.recordState) {
                initRecorder()
//                startRecordThread()
                binding.stopBtRunRecord.text = "stop"
                binding.saveBtRunRecord.visibility = View.INVISIBLE
                binding.saveBtRunRecord.isEnabled = false
            } else {
                binding.stopBtRunRecord.text = "start"
                binding.saveBtRunRecord.visibility = View.VISIBLE
                binding.saveBtRunRecord.isEnabled = true
            }
        } else {
            // 正常启动绘制
            initRecorder()
            binding.stopBtRunRecord.text = "start"
            binding.saveBtRunRecord.visibility = View.VISIBLE
            binding.saveBtRunRecord.isEnabled = true
//            Recognize.reset()
//            Recognize.startDecode()
//            startAsrThread()
        }
    }

    private fun initRecorder() {
        // buffer size in bytes 1280
        miniBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
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
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            miniBufferSize
        )
        Log.i(LOG_TAG, "Record init okay")
        if (model.record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!")
        }
    }

    private fun startRecordThread() {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
        model.record.startRecording()
        binding.stopBtRunRecord.text = "stop"
        model.produceAudio()
    }


    companion object {
        // record
        private const val SAMPLE_RATE = 16000 // The sampling rate
    }
}
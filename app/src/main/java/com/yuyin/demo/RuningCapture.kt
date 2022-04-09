package com.yuyin.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningCaptureBinding
import com.yuyin.demo.models.RunningCaptureViewModel
import com.yuyin.demo.models.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RuningCapture : Fragment() {
    private val LOG_TAG = "YUYIN_RECORD"
    private var _binding: FragmentRuningCaptureBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    private val flowView = EasyFloat.getFloatView("Capture")!!
        .findViewById<TextView>(R.id.flow_text)

    private lateinit var m_mediaProjectionManager: MediaProjectionManager

    // ViewModel
    private val model: RunningCaptureViewModel by viewModels()

    private val yuYinModel: YuyinViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRuningCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRunner()

        model.viewModelScope.launch(Dispatchers.IO) {
            YuYinUtil.prepareModel(requireActivity() as MainActivityView)
            Recognize.init(yuYinModel.model_path, yuYinModel.dic_path) // 初始化模型
            // 订阅结果
            model.updateFlow(flowView, recyclerView)
        }

        binding.stopBtRunCap.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Main) {
                        binding.stopBtRunCap.isEnabled = true
                        model.recordState = false
                        model.asrState = false
                        flowView.text = ""
                    }
                    model.record.stop()
                    Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.stopBtRunCap.text = "start"
                        binding.saveBtRunCap.visibility = View.VISIBLE
                        binding.saveBtRunCap.isEnabled = true
                    }
                }
            } else {
                model.viewModelScope.launch(Dispatchers.Default) {
//                    Recognize.reset()
                    startRecord()
                    withContext(Dispatchers.Main) {
                        model.recordState = true
                        model.asrState = true
                        binding.saveBtRunCap.visibility = View.INVISIBLE
                        binding.saveBtRunCap.isEnabled = false
                    }
                    Recognize.startDecode()
                    model.getTextFlow()
                }
            }
        }
        binding.saveBtRunCap.setOnClickListener { // get all Resukt
            // saveToFile
            save_file(requireContext(), model.speechList)
        }
    }

    private fun startRecord() {
        model.record.startRecording()
        binding.stopBtRunCap.text = "stop"
        model.produceAudio()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        model.change_senor = false //TODO 旋转后适配屏幕
    }

    override fun onDestroyView() {
        super.onDestroyView()

        model.recordState = false
        model.asrState = false
        _binding = null
        Recognize.setInputFinished()
        Recognize.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun initRunner() {
        // 滚动视图
        model.linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding.recyclerRunCap
        recyclerView.layoutManager = model.linearLayoutManager
        recyclerView.adapter = model.adapter
        // false false
        // true true
        if (!model.change_senor) {
            // 正常启动绘制
            initRecorder()
            binding.stopBtRunCap.text = "start"
            binding.saveBtRunCap.visibility = View.VISIBLE
            binding.saveBtRunCap.isEnabled = true
        }

    }



    private fun initRecorder() {
        model.record = yuYinModel.recorder
    }

}
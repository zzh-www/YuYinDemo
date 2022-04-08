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
                    Recognize.reset()
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

    private fun configRecorder(mediaProjection: MediaProjection) {
        // 配置所需录制的音频流
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        // 配置采样格式
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(model.SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        model.miniBufferSize = AudioRecord.getMinBufferSize(
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(LOG_TAG, "Can not init")
            return
        }

        model.record = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(model.miniBufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()


    }


    private fun initRecorder() {

        m_mediaProjectionManager =
            requireActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = m_mediaProjectionManager.createScreenCaptureIntent()

        // 获取录制屏幕权限 并启动服务
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 获取权限成功，初始化 recorder
                val mediaProject = m_mediaProjectionManager.getMediaProjection(-1, result.data!!)
                configRecorder(mediaProject)
            } else {
                // 否则直接回退
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).popBackStack()
            }
        }.launch(intent)

    }


    companion object {
        const val CaptureAudio_ALL = "CaptureAudio"
        const val CaptureAudio_START = "CaptureAudio_START"
        const val CaptureAudio_RESTART_RECORDING = "CaptureAudio_RESTART_RECORDING"
        const val CaptureAudio_START_ASR = "CaptureAudio_START_ASR"
        const val CaptureAudio_STOP = "CaptureAudio_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_CaptureAudio_NAME = "CaptureAudio_NAME"
        const val m_CREATE_SCREEN_CAPTURE = 1001
        const val EXTRA_ACTION_NAME = "ACTION_NAME"
        const val ACTION_ALL = "ALL"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_RECORDING = "CaptureAudio_START_RECORDING"
        const val ACTION_STOP_RECORDING = "CaptureAudio_STOP_RECORDING"
        const val ACTION_STOP_RECORDING_From_Notification =
            "ACTION_STOP_RECORDING_From_Notification"
        const val ACTION_STOP_RECORDING_To_Main = "CaptureAudio_STOP_RECORDING_To_Main"
        const val ACTION_START_RECORDING_From_Notification =
            "CaptureAudio_START_RECORDING_From_Notification"

        // view
        private const val LOG_TAG = "YUYIN_RECORD"
    }
}
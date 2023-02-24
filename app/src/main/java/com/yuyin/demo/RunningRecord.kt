package com.yuyin.demo

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningRecordBinding
import com.yuyin.demo.models.RunningRecordViewModel
import com.yuyin.demo.models.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yuyin.demo.YuYinUtil.YuYinLog as Log


class RunningRecord : Fragment() {

    companion object {
        const val tag = "YUYIN_RECORD"
    }

    private var _binding: FragmentRuningRecordBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

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

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.run_asr_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.save_option -> {
                        save_file(requireContext(), model.speechList)
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRunner()
        // init model
        val floatView = EasyFloat.getFloatView(MainActivityView.floatTag)!!.findViewById<TextView>(R.id.flow_text)
        model.viewModelScope.launch(Dispatchers.IO) {
            YuYinUtil.prepareModel(requireActivity() as MainActivityView)
            Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)  // 初始化模型
            initModel = true
            withContext(Dispatchers.Main) {
                // 订阅结果
                model.updateFlow(floatView, recyclerView)
            }
        }
        binding.runRecordBt.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.isEnabled = true
                        model.recordState = false
                        model.asrState = false
                        floatView.text = ""
                    }
                    model.record.stop()
                    if (!Recognize.getInit())
                        Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.text = "start"
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
                    }
                    Recognize.startDecode()
                    model.getTextFlow()
                }
            }
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
        initRecorder()
        binding.runRecordBt.text = "start"
    }

    private fun initRecorder() {
        // buffer size in bytes 1280
        model.miniBufferSize = AudioRecord.getMinBufferSize(
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (model.miniBufferSize == AudioRecord.ERROR || model.miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(tag, "Audio buffer can't initialize!")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(tag, "Audio Record can't initialize for no permission")
            return
        }
        model.record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            model.miniBufferSize
        )
        Log.i(tag, "Record init okay")
        if (model.record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(tag, "Audio Record can't initialize!")
        }
    }

    private fun startRecord() {
//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
        model.record.startRecording()
        binding.runRecordBt.text = "stop"
        model.produceAudio()
    }


}
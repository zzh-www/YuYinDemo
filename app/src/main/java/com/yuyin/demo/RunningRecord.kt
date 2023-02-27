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
import androidx.appcompat.content.res.AppCompatResources
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
import com.yuyin.demo.databinding.FragmentRunningAsrBinding
import com.yuyin.demo.models.RunningRecordViewModel
import com.yuyin.demo.models.YuyinViewModel
import com.yuyin.demo.view.speech.SpeechText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yuyin.demo.YuYinUtil.YuYinLog as Log


class RunningRecord : Fragment() {

    companion object {
        const val tag = "YUYIN_RECORD"
    }
    private var _binding: FragmentRunningAsrBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    // ViewModel
    private val model: RunningRecordViewModel by viewModels()

    private val yuYinModel: YuyinViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRunningAsrBinding.inflate(inflater, container, false)

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
                        model.viewModelScope.launch(Dispatchers.IO) {
                            val result = StringBuilder()
                            for (i in model.speechList) {
                                result.append(i.text)
                                result.append("\n")
                            }
                            if (!binding.runRecordHotView.text.isNullOrBlank()) {
                                result.append(binding.runRecordHotView.text.toString())
                            }
                            save_file(
                                requireContext(),
                                result.toString(),
                                binding.titleText.toString()
                            )
                        }
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
        binding.runRecordBt.isEnabled = false
        model.viewModelScope.launch(Dispatchers.IO) {
            YuYinUtil.prepareModel(requireActivity() as MainActivityView)
            Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)  // 初始化模型
            withContext(Dispatchers.Main) {
                // 订阅结果
                binding.runRecordBt.isEnabled = true
                model.updateFlow(floatView, recyclerView, binding.runRecordHotView)
            }
        }
        binding.runRecordBt.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.isEnabled = false
                        model.recordState = false
                        model.asrState = false
                        floatView.text = ""
                    }
                    model.record?.stop()
                    if (!Recognize.getFinished())
                        Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.text = requireContext().getString(R.string.start)
                        binding.runRecordBt.icon = AppCompatResources.getDrawable(requireContext(),R.drawable.play_icon36)
                        binding.runRecordBt.isEnabled = true
                        if (!binding.runRecordHotView.text.isNullOrBlank()) {
                            model.speechList.add(SpeechText(binding.runRecordHotView.text.toString()))
                            model.adapter.notifyItemInserted(model.speechList.size)
                            recyclerView.scrollToPosition(model.speechList.size)
                        }
                    }
                }
            } else {
                model.viewModelScope.launch(Dispatchers.IO) {
                    if (Recognize.getInit()) {
                        withContext(Dispatchers.Main) {
                            binding.runRecordBt.isEnabled = false
                        }
                        Recognize.reset()
                        startRecord()
                        withContext(Dispatchers.Main) {
                            model.recordState = true
                            model.asrState = true
                        }
                        Recognize.startDecode()
                        model.getTextFlow()
                        withContext(Dispatchers.Main) {
                            binding.runRecordBt.text = requireContext().getString(R.string.stop)
                            binding.runRecordBt.icon = AppCompatResources.getDrawable(requireContext(),R.drawable.stop_icon36)
                            binding.runRecordBt.isEnabled = true
                        }
                    } else {
                        YuYinUtil.prepareModel(requireActivity() as MainActivityView)
                        Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)
                    }
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
        model.record?.release() // 由当前fragment创建
        model.record = null
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
        binding.runRecordBt.text = this.getString(R.string.start)
        binding.runRecordBt.icon =  AppCompatResources.getDrawable(requireContext(),R.drawable.play_icon36)
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
        if (model.record?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(tag, "Audio Record can't initialize!")
        }
    }

    private fun startRecord() {
        model.record?.startRecording()
        model.produceAudio()
    }


}
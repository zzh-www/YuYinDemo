package com.yuyin.demo

import android.content.res.Configuration
import android.media.AudioRecord
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
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
import com.yuyin.demo.models.RunningCaptureViewModel
import com.yuyin.demo.models.YuyinViewModel
import com.yuyin.demo.view.speech.SpeechText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yuyin.demo.YuYinUtil.YuYinLog as Log

class RuningCapture : Fragment() {

    companion object {
        const val tag = "YUYIN_RECORD"
    }

    private var _binding: FragmentRunningAsrBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    // ViewModel
    private val model: RunningCaptureViewModel by viewModels()

    private val yuYinModel: YuyinViewModel by activityViewModels()

    private var startModel = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRunningAsrBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(object : MenuProvider {
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
        val floatView = EasyFloat.getFloatView(MainActivityView.floatTag)!!
            .findViewById<TextView>(R.id.flow_text)
        initRunner()
        binding.runRecordBt.isEnabled = false
        model.viewModelScope.launch(Dispatchers.IO) {
            Log.e(tag, "${Recognize.getInit()} init model")
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
                binding.runRecordBt.isEnabled = false
                model.viewModelScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.isEnabled = true
                        model.recordState = false
                        model.asrState = false
                        floatView.text = ""
                    }
                    model.record.stop()
                    if (!Recognize.getFinished())
                        Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.text = requireContext().getString(R.string.start)
                        binding.runRecordBt.icon =
                            AppCompatResources.getDrawable(requireContext(), R.drawable.play_icon36)
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
                    // 确保上一轮次确实已经结束
                    //TODO 可以考虑不终止转录 只终止record
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
                            binding.runRecordBt.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.stop_icon36
                            )
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

    private fun startRecord() {
        model.record.startRecording()
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
        if (model.record.state == AudioRecord.STATE_INITIALIZED) {
            model.record.stop()
        }
        _binding = null
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
        binding.runRecordBt.icon =
            AppCompatResources.getDrawable(requireContext(), R.drawable.play_icon36)
    }


    private fun initRecorder() {
        model.record = yuYinModel.recorder!!
    }

}
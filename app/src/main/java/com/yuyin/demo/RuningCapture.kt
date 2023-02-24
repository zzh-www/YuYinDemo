package com.yuyin.demo

import android.content.res.Configuration
import android.media.AudioRecord
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
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
import com.yuyin.demo.YuYinUtil.YuYinLog as Log

class RuningCapture : Fragment() {

    companion object {
        const val tag = "YUYIN_RECORD"
    }

    private var _binding: FragmentRuningCaptureBinding? = null
    private val binding get() = _binding!!

    // 滚动视图
    private lateinit var recyclerView: RecyclerView

    // ViewModel
    private val model: RunningCaptureViewModel by viewModels()

    private val yuYinModel: YuyinViewModel by activityViewModels()

    private var initModel = false

    private var startModel = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRuningCaptureBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(object : MenuProvider {
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

        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val floatView = EasyFloat.getFloatView(MainActivityView.floatTag)!!.findViewById<TextView>(R.id.flow_text)

        initRunner()

        model.viewModelScope.launch(Dispatchers.IO) {
            if (!initModel) {
                Log.e(tag, "${Recognize.getInit()} init model")
                YuYinUtil.prepareModel(requireActivity() as MainActivityView)
                Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)  // 初始化模型
                initModel = true
                withContext(Dispatchers.Main) {
                    // 订阅结果
                    model.updateFlow(floatView, recyclerView)
                }
            }
        }



        binding.stopBtRunCap.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.Main) {
                    if (!initModel && Recognize.getInit()) {
                        //延迟10毫秒
                        withContext(Dispatchers.Main) {
                            binding.stopBtRunCap.isEnabled = true
                            model.recordState = false
                            model.asrState = false
                            floatView.text = ""
                        }
                        model.record.stop()
//                        if (!Recognize.getFinished())
                        // 调用的条件是 必须为false 因为就是为了设置为false
//                            Recognize.setInputFinished()
                        withContext(Dispatchers.Main) {
                            binding.stopBtRunCap.text = "start"
                            binding.saveBtRunCap.visibility = View.VISIBLE
                            binding.saveBtRunCap.isEnabled = true
                        }
                    } else {
                        YuYinUtil.prepareModel(requireActivity() as MainActivityView)
                        Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)
                    }
                }
            } else {
                model.viewModelScope.launch(Dispatchers.IO) {
                    // 确保上一轮次确实已经结束
                    //TODO 可以考虑不终止转录 只终止record
                    if (!initModel && Recognize.getInit()) {
//                        Recognize.reset()
                        startRecord()
                        withContext(Dispatchers.Main) {
                            model.recordState = true
                            model.asrState = true
                            binding.saveBtRunCap.visibility = View.INVISIBLE
                            binding.saveBtRunCap.isEnabled = false
                        }
                        if (startModel == false) {
                            Recognize.startDecode()
                            startModel = true
                        }
//                        Recognize.startDecode()
                        model.getTextFlow()
                    } else {
                        YuYinUtil.prepareModel(requireActivity() as MainActivityView)
                        Recognize.init(yuYinModel.model_path, yuYinModel.dic_path)
                    }
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
        model.record = yuYinModel.recorder!!
    }

}
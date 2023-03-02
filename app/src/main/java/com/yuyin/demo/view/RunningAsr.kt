package com.yuyin.demo.view


import android.content.res.Configuration
import android.media.AudioRecord
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.R
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRunningAsrBinding
import com.yuyin.demo.viewmodel.RunningAsrViewModel
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yuyin.demo.YuYinUtil.YuYinLog as Log


open class RunningAsr : Fragment() {

    open val mTAG = "YUYIN_ASR"
    private var _binding: FragmentRunningAsrBinding? = null
    val binding get() = _binding!!
    lateinit var record: AudioRecord

    // 滚动视图
    lateinit var recyclerView: RecyclerView

    // ViewModel
    open val model: RunningAsrViewModel by viewModels()

    val yuYinModel: YuyinViewModel by activityViewModels()

    lateinit var floatView: StrokeView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRunningAsrBinding.inflate(inflater, container, false)
        initMenu()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRunner()
        // init model
        floatView = EasyFloat.getFloatView(MainActivityView.floatTag)!!.findViewById(R.id.flow_text)
        binding.runRecordBt.isEnabled = false
        initAsrModel()
        initPlayButton()
        editModeForRecyclerView()
        initFloatingBt()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i(mTAG,"onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        model.change_senor = false // 标记屏幕旋转
    }

    override fun onDestroyView() {
        Log.i(mTAG,"onDestroyView")
        super.onDestroyView()
        _binding = null
        destroyRecord()
    }

    override fun onDestroy() {
        Log.i(this.mTAG,"onDestroy")
        super.onDestroy()
    }

    open fun initRunner() {
        // 滚动视图
        model.linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding.recyclerRunRecord
        recyclerView.layoutManager = model.linearLayoutManager
        recyclerView.adapter = model.adapter
        // false false
        // true true
        initRecorder()
        binding.runRecordBt.text = this.getString(R.string.start)
        binding.runRecordBt.icon =  AppCompatResources.getDrawable(requireContext(),
            R.drawable.play_icon36
        )
    }

    open fun initRecorder() {
        Log.e(mTAG,"need override")
    }

    open fun startRecord() {
        Log.e(mTAG,"need override")
    }

    open fun destroyRecord() {
        Log.e(mTAG,"need override")
    }

    private fun initMenu() {
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
                                binding.titleText.text.toString()
                            )
                        }
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initPlayButton() {
        binding.runRecordBt.setOnClickListener {
            if (model.recordState) {
                model.viewModelScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.isEnabled = false
                        model.recordState = false
                        model.asrState = false
                        floatView.text = ""
                    }
                    record?.stop()
                    if (!Recognize.getFinished())
                        Recognize.setInputFinished()
                    withContext(Dispatchers.Main) {
                        binding.runRecordBt.text = requireContext().getString(R.string.start)
                        binding.runRecordBt.icon = AppCompatResources.getDrawable(requireContext(),
                            R.drawable.play_icon36
                        )
                        binding.runRecordBt.isEnabled = true
                        if (!binding.runRecordHotView.text.isNullOrBlank()) {
                            model.updateSpeechList(recyclerView,binding.runRecordHotView.text.toString())
                            binding.runRecordHotView.text = ""
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
                            binding.runRecordBt.icon = AppCompatResources.getDrawable(requireContext(),
                                R.drawable.stop_icon36
                            )
                            binding.runRecordBt.isEnabled = true
                        }
                    } else {
                        Recognize.init(yuYinModel.modelPath, yuYinModel.dicPath)
                    }
                }
            }
        }
    }

    private fun initAsrModel() {
        model.viewModelScope.launch(Dispatchers.IO) {
            Recognize.init(yuYinModel.modelPath, yuYinModel.dicPath)  // 初始化模型
            withContext(Dispatchers.Main) {
                // 订阅结果
                binding.runRecordBt.isEnabled = true
                model.updateFlow(floatView, recyclerView, binding.runRecordHotView)
            }
        }
    }

    private fun editModeForRecyclerView() {
        model.viewModelScope.launch(Dispatchers.Main) {
            model.canScroll.collect {
                // 不可自动滚动时，展示按钮方便用户点击滚到底部，恢复自动滚动
                if (!it) {
                    binding.goDownBt.show()
                }
            }
        }
    }

    private fun initFloatingBt() {
        binding.goDownBt.hide()
        binding.goDownBt.setOnClickListener {it as FloatingActionButton
            val position = model.speechList.size
            recyclerView.smoothScrollToPosition(position)
            it.hide()
            model.viewModelScope.launch(Dispatchers.Main) {
                // 点击按钮后，启动自动滚动
                model.canScroll.emit(true)
            }
        }
    }
}
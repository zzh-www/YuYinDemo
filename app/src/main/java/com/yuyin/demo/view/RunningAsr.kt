package com.yuyin.demo.view


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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        observeFinish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i(mTAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        model.change_senor = false // 标记屏幕旋转
    }

    override fun onDestroyView() {
        Log.i(mTAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
        destroyRecord()
        if (model.asrState && !model.recordState && !Recognize.getFinished()) {
            Recognize.setInputFinished()
        }
    }

    override fun onDestroy() {
        Log.i(this.mTAG, "onDestroy")
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
        binding.runRecordBt.icon = AppCompatResources.getDrawable(
            requireContext(), R.drawable.play_icon36
        )
    }

    open fun initRecorder() {
        Log.e(mTAG, "need override")
    }

    open fun startRecord() {
        Log.e(mTAG, "need override")
    }

    open fun destroyRecord() {
        Log.e(mTAG, "need override")
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
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
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
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    binding.runRecordBt.isEnabled = false
                    model.recordState = false
                    model.asrState = false
                    floatView.text = ""
                    record.stop()
                    if (!Recognize.getFinished())
                        Recognize.setInputFinished()
                    else
                        model.isModelFinish.emit(true)
                    binding.runRecordBt.text = requireContext().getString(R.string.start)
                    binding.runRecordBt.icon = AppCompatResources.getDrawable(
                        requireContext(), R.drawable.play_icon36
                    )
                    if (!binding.runRecordHotView.text.isNullOrBlank()) {
                        binding.runRecordHotView.text = "****wait for last result****"
                    }
                }
            } else {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (model.isModelInit.value && Recognize.getFinished()) {
                        binding.runRecordBt.isEnabled = false
                        Recognize.reset()
                        startRecord()
                        model.recordState = true
                        model.asrState = true
                        Recognize.startDecode()
                        withContext(Dispatchers.Main) {
                            binding.runRecordBt.text = requireContext().getString(R.string.stop)
                            binding.runRecordBt.icon = AppCompatResources.getDrawable(
                                requireContext(), R.drawable.stop_icon36
                            )
                            binding.runRecordBt.isEnabled = true
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            Recognize.init(yuYinModel.modelPath, yuYinModel.dicPath)
                        }
                    }
                }
            }
        }
    }

    private fun initAsrModel() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            Recognize.setOnNativeAsrModelCall(model.asrListener)
            Recognize.init(yuYinModel.modelPath, yuYinModel.dicPath)  // 初始化模型
            model.isModelInit.collect {
                if (it) {
                    withContext(Dispatchers.Main) {
                        // 订阅结果
                        binding.runRecordBt.isEnabled = true
                        model.run {
                            updateFlowText(floatView)
                            updateHotTextView(binding.runRecordHotView)
                            updateSpeechList(recyclerView)
                        }
                    }
                }
            }
        }
    }

    private fun editModeForRecyclerView() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
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
        binding.goDownBt.setOnClickListener {
            it as FloatingActionButton
            val position = model.speechList.size
            recyclerView.smoothScrollToPosition(position)
            it.hide()
            model.viewModelScope.launch(Dispatchers.Main) {
                // 点击按钮后，启动自动滚动
                model.canScroll.emit(true)
            }
        }
    }

    private fun observeFinish() {
        model.run {
            // 与界面元素相关都应该使用 viewLifecycleOwner.lifecycleScope
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                isModelFinish.collect {
                    if (it) {
                        // 停止转写后收到finish为true事件
                        if (!asrState && !recordState) {
                            withContext(Dispatchers.Main) {
                                binding.runRecordBt.isEnabled = true
                                binding.runRecordHotView.text = ""
                            }
                            updateOffsetTime()
                        }
                    }
                }
            }
        }
    }

    fun updateSpeechList(recyclerView: RecyclerView) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            model.results.filter {
                it.text.isNotEmpty() && (it.state == 2 || it.state == 4)
            }.collect {
                val position = model.speechList.size - 1
                model.speechList.add(it) // add new para
                model.adapter.notifyItemInserted(position + 1)
                if (model.canScroll.value) {
                    // 可滚动才可自动滚动
                    recyclerView.scrollToPosition(position + 1)
                }
                Log.i(tag, "updateSpeechList $it")
            }
        }
    }

    fun updateFlowText(flowText: StrokeView) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            model.hotResult.collectLatest {
                flowText.text = it
                Log.i(tag, "update flowText $it")
            }
        }
    }

    fun updateHotTextView(hotText: TextView) {
        // 当流代表部分操作结果或操作状态更新时，可能没有必要处理每个值，而是只处理最新的那个。
        // 当收集器处理它们太慢的时候， conflate 操作符可以用于跳过中间值。
        // 另一种方式是取消缓慢的收集器，并在每次发射新值的时候重新启动它。
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            model.hotResult.collectLatest {
                hotText.text = it
                val offset = hotText.lineCount * hotText.lineHeight
                if (offset > hotText.height) {
                    hotText.scrollTo(0, offset - hotText.height)
                }
                Log.i(tag, "update hotText $it")
            }
        }
    }

}
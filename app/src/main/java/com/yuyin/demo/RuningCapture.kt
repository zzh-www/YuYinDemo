package com.yuyin.demo

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzf.easyfloat.EasyFloat
import com.mobvoi.wenet.MediaCaptureService
import com.mobvoi.wenet.MediaCaptureService.mcs_Binder
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.get_all_result
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningCaptureBinding
import com.yuyin.demo.models.YuyinViewModel

class RuningCapture : Fragment() {
    private var binding: FragmentRuningCaptureBinding? = null
    private lateinit var speechList: ArrayList<SpeechText>
    private var adapter: SpeechTextAdapter? = null
    private var recyclerView: RecyclerView? = null

    // ViewModel
    private lateinit var model: YuyinViewModel
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            model.mcs_binder = service as mcs_Binder
            model.mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            model.mBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRuningCaptureBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRunner()
        binding!!.stopBtRunCap.setOnClickListener {
            //TODO stopASR
            if (model.startRecord) {
                requireActivity().runOnUiThread {
                    binding!!.stopBtRunCap.isEnabled = false
                    stopRecording()
                    binding!!.stopBtRunCap.text = "start"
                    binding!!.saveBtRunCap.visibility = View.VISIBLE
                    binding!!.saveBtRunCap.isEnabled = true
                }
            } else {
                requireActivity().runOnUiThread {
                    binding!!.stopBtRunCap.isEnabled = false
                    restartRecording()
                    binding!!.stopBtRunCap.text = "stop"
                    binding!!.saveBtRunCap.visibility = View.INVISIBLE
                    binding!!.saveBtRunCap.isEnabled = false
                }
            }
        }
        binding!!.saveBtRunCap.setOnClickListener { // get all Resukt
            get_all_result(speechList)
            // saveToFile
            save_file(model.context, speechList)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        model.change_senor = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().runOnUiThread {
            model.startRecord = false
            model.startAsr = false
            binding = null
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        model = ViewModelProvider(requireActivity())[YuyinViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().runOnUiThread {
            stopRecordingToActivity()
            model.startAsr = false
            Recognize.setInputFinished()
        }
    }

    // 广播服务
    // init view
    private fun initRunner() {


        // 滚动视图
        val linearLayoutManager = LinearLayoutManager(model.context)
        speechList = ArrayList()
        recyclerView = binding!!.recyclerRunCap
        recyclerView!!.layoutManager = linearLayoutManager
        if (model.getResultsSize() == null || model.getResultsSize()!! < 1) {
            speechList.add(SpeechText("Hi"))
            //            speechList = model.getResults().getValue();
        } else {
            speechList = model.results.value!!
        }
        adapter = SpeechTextAdapter(speechList)
        recyclerView!!.adapter = adapter
        if (model.change_senor) {
            if (model.startRecord) {
                //TODO start service is always start
                initAudioCapture()
                binding!!.stopBtRunCap.text = "stop"
                binding!!.saveBtRunCap.visibility = View.INVISIBLE
                binding!!.saveBtRunCap.isEnabled = false
            } else {
                binding!!.stopBtRunCap.text = "start"
                binding!!.saveBtRunCap.visibility = View.VISIBLE
                binding!!.saveBtRunCap.isEnabled = true
            }
        } else {
            model.startAsr = false
            if (model.mBound) {
                // restart
                Recognize.reset()
                model.startAsr = true
                restartRecording()
                startAsrThread()
                Recognize.startDecode()
            } else {
                initAudioCapture() // start service
            }
            model.context.runOnUiThread {
                binding!!.stopBtRunCap.text = "stop"
                binding!!.saveBtRunCap.visibility = View.INVISIBLE
                binding!!.saveBtRunCap.isEnabled = false
            }
        }
    }

    private fun startRecordingService() {
        try {
            val broadCastIntent = Intent()
            broadCastIntent.action = ACTION_ALL
            broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START)
            model.context.sendBroadcast(broadCastIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartRecording() {
        val broadCastIntent = Intent()
        broadCastIntent.action = ACTION_ALL
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_START_RECORDING)
        model.context.sendBroadcast(broadCastIntent)
    }

    private fun stopRecording() {
        val broadCastIntent = Intent()
        broadCastIntent.action = ACTION_ALL
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP_RECORDING)
        model.context.sendBroadcast(broadCastIntent)
    }

    private fun stopRecordingToActivity() {
        val broadCastIntent = Intent()
        broadCastIntent.action = ACTION_ALL
        broadCastIntent.putExtra(EXTRA_ACTION_NAME, ACTION_STOP_RECORDING_To_Main)
        model.context.sendBroadcast(broadCastIntent)
    }

    private fun initAudioCapture() {

        // 未启动服务
        if (!model.mBound) {
            // Service
            val m_mediaProjectionManager =
                model.context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = m_mediaProjectionManager.createScreenCaptureIntent()
            // 获取录制屏幕权限 并启动服务
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Thread {
                        val filter = IntentFilter()
                        filter.addAction(CaptureAudio_ALL)
                        model.m_actionReceiver = CaptureAudioReceiver()
                        model.context.registerReceiver(model.m_actionReceiver, filter)
                        val i = Intent(model.context, MediaCaptureService::class.java)
                        model.context.bindService(i, connection, Context.BIND_AUTO_CREATE)
                    }.start()
                    // 启动服务
                    val i = Intent(model.context, MediaCaptureService::class.java)
                    i.action = ACTION_ALL
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.putExtra(EXTRA_RESULT_CODE, m_CREATE_SCREEN_CAPTURE)
                    i.putExtras(result.data!!)
                    model.context.startService(i)
                } else {
                    findNavController(
                        model.context,
                        R.id.yuyin_nav_host_container_fragment
                    ).popBackStack()
                }
            }.launch(intent)
        }
    }

    // Asr
    private fun startAsrThread() {
        Thread {
            while (model.startAsr) {
                try {
                    if (binding == null) break
                    val data = model.mcs_binder!!.audioQueue
                    Recognize.acceptWaveform(data)

                    // 2. get partial result
                    val result = Recognize.getResult()
                    if (Recognize.getResult() === "") {
                        model.context.runOnUiThread {
                            val floatText = EasyFloat.getFloatView("Capture")!!
                                .findViewById<TextView>(R.id.flow_text)
                            floatText.text = result
                        }
                    }
                    if (result!!.endsWith(" ")) {
                        model.context.runOnUiThread {
                            speechList[speechList.size - 1].text = result.trim { it <= ' ' }
                            adapter!!.notifyItemChanged(speechList.size - 1)
                            speechList.add(SpeechText("..."))
                            adapter!!.notifyItemInserted(speechList.size - 1)
                            recyclerView!!.scrollToPosition(speechList.size - 1)
                        }
                    } else {
                        model.context.runOnUiThread {
                            speechList[speechList.size - 1].text = result
                            adapter!!.notifyItemChanged(speechList.size - 1)
                            //                            recyclerView.scrollToPosition(speechList.size()-1);
                            val floatText = EasyFloat.getFloatView("Capture")!!
                                .findViewById<TextView>(R.id.flow_text)
                            floatText.text = result
                        }
                        // 部分结果
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, e.message!!)
                    Log.e(LOG_TAG + "GETWRONG", "runonui")
                    e.printStackTrace()
                }
            }
        }.start()
        model.startAsr = false
    }

    // 不可以耗时操作  在主线程中
    inner class CaptureAudioReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(CaptureAudio_ALL, ignoreCase = true)) {
                val actionName = intent.getStringExtra(EXTRA_CaptureAudio_NAME)
                if (actionName != null && actionName.isNotEmpty()) {
                    if (actionName.equals(CaptureAudio_START, ignoreCase = true)) {

                        // 接收服务已启动的通知
                        // 通知服务开启录制
                        startRecordingService()
                    } else if (actionName.equals(CaptureAudio_START_ASR, ignoreCase = true)) {
                        // binding 变为了null？？？
                        model.context.runOnUiThread {
                            model.context.findViewById<View>(R.id.stop_bt_run_cap).isEnabled =
                                true
                            model.startRecord = true
                        }
                        // 接收录制已启动通知 只有第一次需要
                        if (!model.startAsr) {
                            Recognize.reset()
                            startAsrThread()
                            Recognize.startDecode()
                            model.startAsr = true
                        }
                    } else if (actionName.equals(CaptureAudio_STOP, ignoreCase = true)) {
                        model.context.runOnUiThread {
                            model.startRecord = false
                            val floatText = EasyFloat.getFloatView("Capture")!!
                                .findViewById<TextView>(R.id.flow_text)
                            floatText.text = ""
                            // 跳出当前fragment后
                            try {
                                model.context.findViewById<View>(R.id.stop_bt_run_cap).isEnabled =
                                    true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else if (actionName.equals(
                            CaptureAudio_RESTART_RECORDING,
                            ignoreCase = true
                        )
                    ) {
                        model.context.runOnUiThread {
                            model.context.findViewById<View>(R.id.stop_bt_run_cap).isEnabled =
                                true
                            model.startRecord = true
                        }
                        if (!model.startAsr) {
                            model.startAsr = true
                            startAsrThread()
                        }
                    } else if (actionName.equals(
                            ACTION_STOP_RECORDING_From_Notification,
                            ignoreCase = true
                        )
                    ) {
                        model.context.runOnUiThread {
                            binding!!.stopBtRunCap.isEnabled = false
                            stopRecording()
                            binding!!.stopBtRunCap.text = "start"
                            binding!!.saveBtRunCap.visibility = View.VISIBLE
                            binding!!.saveBtRunCap.isEnabled = true
                        }
                    } else if (actionName.equals(
                            ACTION_START_RECORDING_From_Notification,
                            ignoreCase = true
                        )
                    ) {
                        model.context.runOnUiThread {
                            binding!!.stopBtRunCap.isEnabled = false
                            restartRecording()
                            binding!!.stopBtRunCap.text = "stop"
                            binding!!.saveBtRunCap.visibility = View.INVISIBLE
                            binding!!.saveBtRunCap.isEnabled = false
                        }
                    }
                }
            }
        }
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
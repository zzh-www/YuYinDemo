package com.yuyin.demo

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.YuYinUtil.get_all_result
import com.yuyin.demo.YuYinUtil.save_file
import com.yuyin.demo.databinding.FragmentRuningRecordBinding

class RunningRecord : Fragment() {
    private val LOG_TAG = "YUYIN_RECORD"
    private var binding: FragmentRuningRecordBinding? = null
    private var record: AudioRecord? = null
    private var miniBufferSize = 0

    // 滚动视图
    private var linearLayoutManager: LinearLayoutManager? = null
    private var speechList = ArrayList<SpeechText>()
    private var adapter: SpeechTextAdapter? = null
    private var recyclerView: RecyclerView? = null

    // ViewModel
    private lateinit var model: YuyinViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRuningRecordBinding.inflate(inflater, container, false)
        return binding!!.root
        //        return inflater.inflate(R.layout.fragment_runing_record, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRunner()
        binding!!.stopBtRunRecord.setOnClickListener {
            //只需停止录音即可
            if (model.startRecord) {
                requireActivity().runOnUiThread {
                    binding!!.stopBtRunRecord.isEnabled = false
                    model.startRecord = false
                    binding!!.stopBtRunRecord.text = "start"
                    binding!!.saveBtRunRecord.visibility = View.VISIBLE
                    binding!!.saveBtRunRecord.isEnabled = true
                }
            } else {
                initRecorder()
                startRecordThread()
                model.startRecord = true
                binding!!.saveBtRunRecord.visibility = View.INVISIBLE
                binding!!.saveBtRunRecord.isEnabled = false
                // Recognize.startDecode();
                // startAsrThread();
            }
        }
        binding!!.saveBtRunRecord.setOnClickListener { // get all Result
            get_all_result(speechList)
            // saveToFile
            save_file(requireContext(), speechList)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        model.change_senor = true // 标记屏幕旋转
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().runOnUiThread {
            model.startRecord = false
            model.startAsr = false
            binding = null
        }
        model.results.value = speechList
    }

    override fun onDestroy() {
        Recognize.setInputFinished()
        model.results.value!!.clear()
        model.bufferQueue.clear()
        super.onDestroy()
    }

    private fun initRunner() {
        model = ViewModelProvider(requireActivity())[YuyinViewModel::class.java]

        // 滚动视图
        linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding!!.recyclerRunRecord
        recyclerView!!.layoutManager = linearLayoutManager
        if (model.getResultsSize() == null || model.getResultsSize()!! < 1) {
            speechList.add(SpeechText("Hi"))
        } else {
            speechList = model.results.value!!
        }
        adapter = SpeechTextAdapter(speechList)
        recyclerView!!.adapter = adapter
        // false false
        // true true
        if (model.change_senor) {
            // 屏幕旋转 而重构

            // 旋转前正在录制 应该继续录制
            if (model.startRecord) {
                initRecorder()
                startRecordThread()
                binding!!.stopBtRunRecord.text = "stop"
                binding!!.saveBtRunRecord.visibility = View.INVISIBLE
                binding!!.saveBtRunRecord.isEnabled = false
            } else {
                binding!!.stopBtRunRecord.text = "start"
                binding!!.saveBtRunRecord.visibility = View.VISIBLE
                binding!!.saveBtRunRecord.isEnabled = true
            }
        } else {
            // 正常启动绘制
            initRecorder()
            startRecordThread()
            binding!!.stopBtRunRecord.text = "stop"
            binding!!.saveBtRunRecord.visibility = View.INVISIBLE
            binding!!.saveBtRunRecord.isEnabled = false
            Recognize.reset()
            Recognize.startDecode()
            startAsrThread()
        }
    }

    private fun initRecorder() {
//    // buffer size in bytes 1280
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
        record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            miniBufferSize
        )
        Log.i(LOG_TAG, "Record init okay")
        if (record!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!")
        }
        model.startRecord = true
    }

    private fun startRecordThread() {
//        model.setStartRecord(true);
        Thread {

//      VoiceRectView voiceView = findViewById(R.id.voiceRectView);
            record!!.startRecording()
            requireActivity().runOnUiThread { binding!!.stopBtRunRecord.text = "stop" }
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (model.startRecord) {
                val buffer = ShortArray(miniBufferSize / 2)
                val read = record!!.read(buffer, 0, buffer.size)
                //        voiceView.add(calculateDb(buffer));
                try {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
//                        bufferQueue.put(buffer)
                        model.bufferQueue.put(buffer)
                    }
                } catch (e: InterruptedException) {
                    Log.e(LOG_TAG, e.message!!)
                }
            }
            record!!.stop()
            if (binding != null) {
                requireActivity().runOnUiThread {
                    if (binding != null) binding!!.stopBtRunRecord.isEnabled = true
                }
            }
        }.start()
    }

    private fun startAsrThread() {
        Thread {

            // Send all data
            model.startAsr = true
            while (model.startAsr || model.bufferQueue.size > 0) {
                try {
                    if (binding == null) break
                    val data = model.bufferQueue.take()
                    if (data != null) {
                        // 1. add data to C++ interface
                        Recognize.acceptWaveform(data) // 将音频传到模型
                    }

                    // 2. get partial result
                    val result = Recognize.getResult()
                    if (result == "") continue
                    if (result!!.endsWith(" ")) {
                        requireActivity().runOnUiThread {
                            speechList[speechList.size - 1].text = result.trim { it <= ' ' }
                            adapter!!.notifyItemChanged(speechList.size - 1)
                            speechList.add(SpeechText("..."))
                            adapter!!.notifyItemInserted(speechList.size - 1)
                            recyclerView!!.scrollToPosition(speechList.size - 1)
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            speechList[speechList.size - 1].text = result
                            adapter!!.notifyItemChanged(speechList.size - 1)
                        }
                        // 部分结果
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, e.message!!)
                    Log.e(LOG_TAG + "GETWRONG", "runonui")
                }
            }
        }.start()
    }

    companion object {
        // record
        private const val SAMPLE_RATE = 16000 // The sampling rate
    }
}
package com.yuyin.demo.view

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import com.yuyin.demo.viewmodel.RunningAsrViewModel
import com.yuyin.demo.viewmodel.RunningRecordViewModel
import com.yuyin.demo.YuYinUtil.YuYinLog as Log


class RunningRecord : RunningAsr(){

    override val mTAG = "YUYIN_RECORD"
    override val model: RunningRecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(mTAG, "onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(mTAG,"onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i(mTAG,"onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        model.change_senor = false // 标记屏幕旋转
    }

    override fun onDestroyView() {
        Log.i(mTAG,"onDestroyView")
        super.onDestroyView()
        destroyRecord()
    }

    override fun onDestroy() {
        Log.i(mTAG,"onDestroy")
        super.onDestroy()
    }

    override fun initRecorder() {
        // buffer size in bytes 1280
        model.miniBufferSize = AudioRecord.getMinBufferSize(
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (model.miniBufferSize == AudioRecord.ERROR || model.miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(mTAG, "Audio buffer can't initialize!")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(mTAG, "Audio Record can't initialize for no permission")
            return
        }
        record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            model.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            model.miniBufferSize
        )
        Log.i(mTAG, "Record init okay")
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(mTAG, "Audio Record can't initialize!")
        }
    }

    override fun startRecord() {
        record.let {
            it.startRecording()
            model.produceAudio(it)
        }
    }

    override fun destroyRecord() {
        model.asrState = false
        model.recordState = false
        record.release() // 由当前fragment创建
    }

}
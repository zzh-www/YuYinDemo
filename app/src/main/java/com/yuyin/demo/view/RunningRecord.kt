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
import com.yuyin.demo.utils.YuYinUtil
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_AUDIO_ENCODING
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_CHANNELS
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.RECORDER_SAMPLERATE
import com.yuyin.demo.utils.YuYinUtil.RecordHelper.miniBufferSize
import com.yuyin.demo.viewmodel.RunningRecordViewModel
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log


class RunningRecord : RunningAsr(){

    override val TAG = "YUYIN_RECORD"
    override val model: RunningRecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG,"onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i(TAG,"onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        model.change_senor = false // 标记屏幕旋转
    }

    override fun onDestroyView() {
        Log.i(TAG,"onDestroyView")
        super.onDestroyView()
        destroyRecord()
    }

    override fun onDestroy() {
        Log.i(TAG,"onDestroy")
        super.onDestroy()
    }

    override fun initRecorder() {
        // buffer size in bytes 1280
        miniBufferSize =  AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING
        )
        if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Audio buffer can't initialize!")
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Audio Record can't initialize for no permission")
            return
        }
        record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING,
            miniBufferSize
        )
        Log.i(TAG, "Record init okay")
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!")
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
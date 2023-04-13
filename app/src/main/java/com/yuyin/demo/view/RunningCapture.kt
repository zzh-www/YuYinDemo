package com.yuyin.demo.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.yuyin.demo.viewmodel.RunningCaptureViewModel
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class RunningCapture : RunningAsr() {

    override val TAG = "YUYIN_CAPTURE"

    override val model:RunningCaptureViewModel by viewModels()

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
        yuYinModel.recorder?.let {
            Log.i(TAG,"init record successful")
            record = it
        }
    }

    override fun startRecord() {
        record.startRecording()
        model.produceAudio(record)
    }

    override fun destroyRecord() {
        if (model.recordState) {
            model.recordState = false
            record.stop()
        } else {
            Log.i(TAG,"it is stopped")
        }
    }

}
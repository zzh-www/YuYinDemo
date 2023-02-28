package com.yuyin.demo.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.yuyin.demo.service.MediaCaptureService
import com.yuyin.demo.viewmodel.RunningCaptureViewModel
import com.yuyin.demo.YuYinUtil.YuYinLog as Log

class RuningCapture : RunningAsr() {

    override val mTAG = "YUYIN_CAPTURE"

    override val model:RunningCaptureViewModel by viewModels()

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
        yuYinModel.recorder?.let {
            Log.i(mTAG,"init record successful")
            record = it
        }
        model.miniBufferSize = MediaCaptureService.miniBufferSize
    }

    override fun startRecord() {
        record.startRecording()
        model.produceAudio(record)
    }

    override fun destroyRecord() {
        model.recordState = false
        model.asrState = false
        record.stop()
    }

}
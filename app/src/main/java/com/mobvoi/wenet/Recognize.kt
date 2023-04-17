package com.mobvoi.wenet

import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log
import com.yuyin.demo.models.OnNativeAsrModelCall
import com.yuyin.demo.models.SpeechResult
import java.nio.charset.StandardCharsets

object Recognize {
    const val tag = "Recognize"
    init {
        System.loadLibrary("wenet")
    }

    external fun init(modelPath: String?, dictPath: String?)
    external fun reset()
    external fun acceptWaveform(waveform: ShortArray?)
    external fun setInputFinished()
    external fun getFinished(): Boolean
    external fun startDecode()
    private external fun getByteResult(): ByteArray
    val result: String get() =
        jniArrayToJavaString(getByteResult())
    fun jniArrayToJavaString(array: ByteArray) = String(array, StandardCharsets.UTF_8)  // Android JNI UTF-8编码，JVM UTF-16编码
    external fun javaStringToJniArray(s: String): ByteArray

    val defaultListener = object : OnNativeAsrModelCall {
        override fun onSpeechResultReceive(data: SpeechResult) {
            Log.i(tag,"default listener onSpeechResultReceive")
        }
        override fun onSpeechResultPartReceive(data: ByteArray) {
            Log.i(tag, "default listener onSpeechResultPartReceive")
        }

        override fun onModelInit(isInit: Boolean) {
            Log.i(tag,"default listener onModelInit")
        }

        override fun onModelReset(isReset: Boolean) {
            Log.i(tag,"default listener onModelReset")
        }

        override fun onModelFinish(isFinish: Boolean) {
            Log.i(tag,"default listener onModelFinish")
        }
    }

    private var listener: OnNativeAsrModelCall = defaultListener
    @JvmStatic
    fun resultReceiveListener(data: SpeechResult) {
        listener.onSpeechResultReceive(data)
    }

    @JvmStatic
    fun partResultReceiveListener(data: ByteArray) {
        listener.onSpeechResultPartReceive(data)
    }

    @JvmStatic
    fun modelInitListener(data: Boolean) {
        listener.onModelInit(data)
    }

    @JvmStatic
    fun modelResetListener(data: Boolean) {
        listener.onModelReset(data)
    }

    @JvmStatic
    fun modelFinishListener(data: Boolean) {
        listener.onModelFinish(data)
    }


    @JvmStatic

    fun setOnNativeAsrModelCall(newListener: OnNativeAsrModelCall) {
        this.listener = newListener
    }
}
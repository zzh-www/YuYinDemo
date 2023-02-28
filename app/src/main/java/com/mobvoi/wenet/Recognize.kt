package com.mobvoi.wenet

import java.nio.charset.StandardCharsets

object Recognize {
    init {
        System.loadLibrary("wenet")
    }

    // TODO 重写方法 都要带有返回值做错误处理
    external fun init(modelPath: String?, dictPath: String?)
    external fun reset()
    external fun acceptWaveform(waveform: ShortArray?)
    external fun setInputFinished()
    external fun getFinished(): Boolean
    external fun getInit(): Boolean
    external fun startDecode()
    private external fun getByteResult(): ByteArray
    val result: String get() =
        jniArrayToJavaString(getByteResult())
    fun jniArrayToJavaString(array: ByteArray) = String(array, StandardCharsets.UTF_8)  // Android JNI UTF-8编码，JVM UTF-16编码
    external fun javaStringToJniArray(s: String): ByteArray
}
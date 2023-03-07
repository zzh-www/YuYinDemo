package com.yuyin.demo.models

interface OnNativeAsrModelCall {
    fun onSpeechResultReceive(data: SpeechResult)
    fun onSpeechResultPartReceive(data: ByteArray)
    fun onModelInit(isInit: Boolean)
    fun onModelReset(isReset: Boolean)
    fun onModelFinish(isFinish: Boolean)
}
package com.mobvoi.wenet;

public class Recognize {

    static {
        System.loadLibrary("wenet");
    }


    // TODO 重写方法 都要带有返回值做错误处理
    public static native void init(String modelPath, String dictPath);
    public static native void reset();
    public static native void acceptWaveform(short[] waveform);
    public static native void setInputFinished();
    public static native boolean getFinished();
    public static native boolean getInit();
    public static native void startDecode();
    public static native String getResult();
}
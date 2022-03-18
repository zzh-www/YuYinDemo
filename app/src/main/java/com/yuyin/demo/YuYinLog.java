package com.yuyin.demo;


import android.util.Log;

public class YuYinLog {
    private static final int level = Log.VERBOSE;


    private YuYinLog() {

    }


    public static void v(String tag, String msg) {
        if (level <= Log.VERBOSE) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (level <= Log.DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (level <= Log.INFO) {
            Log.v(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (level <= Log.WARN) {
            Log.v(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (level <= Log.ERROR) {
            Log.v(tag, msg);
        }
    }

}
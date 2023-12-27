package com.binky.audiorecorderdemo;

import android.app.Application;

import com.binky.audiorecorder.AudioRecorder;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AudioRecorder.getInstance().init(this);
    }
}

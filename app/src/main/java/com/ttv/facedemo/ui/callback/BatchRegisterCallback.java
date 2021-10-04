package com.ttv.facedemo.ui.callback;


public interface BatchRegisterCallback {

    void onProcess(int current, int failed, int total);

    void onFinish(int current, int failed, int total, String errMsg);
}

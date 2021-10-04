package com.ttv.facedemo.util.face;

import com.ttv.facedemo.ui.model.CompareResult;

public interface RecognizeCallback {

    void onRecognized(CompareResult compareResult, Integer liveness, boolean similarPass);

    void onNoticeChanged(String notice);
}

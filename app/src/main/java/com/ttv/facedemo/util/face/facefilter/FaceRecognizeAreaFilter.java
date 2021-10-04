package com.ttv.facedemo.util.face.facefilter;


import android.graphics.Rect;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;

import java.util.List;

public class FaceRecognizeAreaFilter implements FaceRecognizeFilter {
    private static final String TAG = "FaceRecognizeAreaFilter";
    private Rect validArea;

    public FaceRecognizeAreaFilter(Rect validArea) {
        this.validArea = validArea;
    }

    @Override
    public void filter(List<FacePreviewInfo> facePreviewInfoList) {
        for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
            if (!facePreviewInfo.isQualityPass()) {
                continue;
            }
            facePreviewInfo.setQualityPass(validArea.contains(facePreviewInfo.getRgbTransformedRect()));
        }
    }
}

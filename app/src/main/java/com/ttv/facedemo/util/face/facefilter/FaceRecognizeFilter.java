package com.ttv.facedemo.util.face.facefilter;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;

import java.util.List;

public interface FaceRecognizeFilter {
    void filter(List<FacePreviewInfo> facePreviewInfoList);
}

package com.ttv.facedemo.ui.model;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;

public class LivenessPreviewInfo {
    private FacePreviewInfo  facePreviewInfo;
    private int rgbLiveness;
    private int irLiveness;

    public LivenessPreviewInfo(FacePreviewInfo facePreviewInfo, int rgbLiveness, int irLiveness) {
        this.facePreviewInfo = facePreviewInfo;
        this.rgbLiveness = rgbLiveness;
        this.irLiveness = irLiveness;
    }

    public FacePreviewInfo getFacePreviewInfo() {
        return facePreviewInfo;
    }

    public void setFacePreviewInfo(FacePreviewInfo facePreviewInfo) {
        this.facePreviewInfo = facePreviewInfo;
    }

    public int getRgbLiveness() {
        return rgbLiveness;
    }

    public void setRgbLiveness(int rgbLiveness) {
        this.rgbLiveness = rgbLiveness;
    }

    public int getIrLiveness() {
        return irLiveness;
    }

    public void setIrLiveness(int irLiveness) {
        this.irLiveness = irLiveness;
    }
}

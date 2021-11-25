package com.ttv.facedemo.util.face.model;

import android.graphics.Rect;

import com.ttv.face.FaceInfo;
import com.ttv.face.FaceResult;
import com.ttv.face.LivenessInfo;

public class FacePreviewInfo {
    private FaceResult faceInfoRgb;
    private Rect rgbTransformedRect;

    private Rect irTransformedRect;
    private int rgbLiveness = LivenessInfo.UNKNOWN;
    private float imageQuality = 0f;
    private boolean recognizeAreaValid;
    private int trackId;
    private boolean qualityPass = true;

    private int mask;
    private Rect foreRect;

    public Rect getForeRect() {
        return foreRect;
    }

    public void setForeRect(Rect foreRect) {
        this.foreRect = foreRect;
    }

    public FacePreviewInfo(FaceResult faceInfoRgb, int trackId) {
        this.faceInfoRgb = faceInfoRgb;
        this.trackId = trackId;
    }

    public FaceResult getFaceInfoRgb() {
        return faceInfoRgb;
    }

    public void setFaceInfoRgb(FaceResult faceInfoRgb) {
        this.faceInfoRgb = faceInfoRgb;
    }


    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public void setRgbTransformedRect(Rect rgbTransformedRect) {
        this.rgbTransformedRect = rgbTransformedRect;
    }

    public void setIrTransformedRect(Rect irTransformedRect) {
        this.irTransformedRect = irTransformedRect;
    }

    public Rect getRgbTransformedRect() {
        return rgbTransformedRect;
    }

    public Rect getIrTransformedRect() {
        return irTransformedRect;
    }

    public boolean isRecognizeAreaValid() {
        return recognizeAreaValid;
    }

    public void setRecognizeAreaValid(boolean recognizeAreaValid) {
        this.recognizeAreaValid = recognizeAreaValid;
    }

    public int getRgbLiveness() {
        return rgbLiveness;
    }

    public void setRgbLiveness(int rgbLiveness) {
        this.rgbLiveness = rgbLiveness;
    }

    public void setImageQuality(float imageQuality) {
        this.imageQuality = imageQuality;
    }

    public float getImageQuality() {
        return imageQuality;
    }

    public boolean isQualityPass() {
        return qualityPass;
    }

    public void setQualityPass(boolean qualityPass) {
        this.qualityPass = qualityPass;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }
}

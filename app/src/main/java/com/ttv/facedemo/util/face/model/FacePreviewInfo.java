package com.ttv.facedemo.util.face.model;

import android.graphics.Rect;

import com.ttv.face.FaceInfo;
import com.ttv.face.LivenessInfo;

public class FacePreviewInfo {
    private FaceInfo faceInfoRgb;
    private FaceInfo faceInfoIr;
    private Rect rgbTransformedRect;

    private Rect irTransformedRect;
    private int rgbLiveness = LivenessInfo.UNKNOWN;
    private int irLiveness = LivenessInfo.UNKNOWN;
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

    public FacePreviewInfo(FaceInfo faceInfoRgb, int trackId) {
        this.faceInfoRgb = faceInfoRgb;
        this.trackId = trackId;
    }

    public FaceInfo getFaceInfoRgb() {
        return faceInfoRgb;
    }

    public void setFaceInfoRgb(FaceInfo faceInfoRgb) {
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

    public void setFaceInfoIr(FaceInfo faceInfoIr) {
        this.faceInfoIr = faceInfoIr;
    }

    public FaceInfo getFaceInfoIr() {
        return faceInfoIr;
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

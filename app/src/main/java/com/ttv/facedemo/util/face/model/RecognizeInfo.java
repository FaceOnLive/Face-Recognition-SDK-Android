package com.ttv.facedemo.util.face.model;

import com.ttv.facedemo.util.face.constants.RequestFeatureStatus;
import com.ttv.face.LivenessInfo;


public class RecognizeInfo {

    private int recognizeStatus = RequestFeatureStatus.TO_RETRY;
    private int extractErrorRetryCount;
    private int liveness = LivenessInfo.UNKNOWN;
    private int livenessErrorRetryCount;
    private String name;
    private Object waitLock = new Object();

    public int getRecognizeStatus() {
        return recognizeStatus;
    }

    public void setRecognizeStatus(int recognizeStatus) {
        this.recognizeStatus = recognizeStatus;
    }

    public void setLiveness(int liveness) {
        this.liveness = liveness;
    }

    public int increaseAndGetExtractErrorRetryCount() {
        return ++extractErrorRetryCount;
    }

    public int getLiveness() {
        return liveness;
    }

    public int increaseAndGetLivenessErrorRetryCount() {
        return ++livenessErrorRetryCount;
    }

    public void setExtractErrorRetryCount(int extractErrorRetryCount) {
        this.extractErrorRetryCount = extractErrorRetryCount;
    }

    public void setLivenessErrorRetryCount(int livenessErrorRetryCount) {
        this.livenessErrorRetryCount = livenessErrorRetryCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getWaitLock() {
        return waitLock;
    }

    public int getExtractErrorRetryCount() {
        return extractErrorRetryCount;
    }

    public int getLivenessErrorRetryCount() {
        return livenessErrorRetryCount;
    }
}
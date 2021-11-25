package com.ttv.facedemo.util.face.model;

import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.face.LivenessParam;

public class RecognizeConfiguration {
    private int extractRetryCount;
    private int livenessRetryCount;
    private int maxDetectFaces;
    private float similarThreshold;
    private float imageQualityNoMaskRecognizeThreshold;
    private float imageQualityMaskRecognizeThreshold;
    private int recognizeFailedRetryInterval;
    private int livenessFailedRetryInterval;
    private boolean enableLiveness;
    private boolean enableImageQuality;
    private boolean enableFaceAreaLimit;
    private boolean keepMaxFace;
    private LivenessParam livenessParam;

    private boolean enableFaceSizeLimit = false;
    private boolean enableFaceMoveLimit = false;
    private int faceSizeLimit = 0;
    private int faceMoveLimit = 0;


    public RecognizeConfiguration(Builder builder) {
        this.extractRetryCount = builder.extractRetryCount;
        this.livenessRetryCount = builder.livenessRetryCount;
        this.livenessFailedRetryInterval = builder.livenessFailedRetryInterval;
        this.maxDetectFaces = builder.maxDetectFaces;
        this.similarThreshold = builder.similarThreshold;
        this.imageQualityNoMaskRecognizeThreshold = builder.imageQualityNoMaskRecognizeThreshold;
        this.imageQualityMaskRecognizeThreshold = builder.imageQualityMaskRecognizeThreshold;
        this.enableLiveness = builder.enableLiveness;
        this.enableImageQuality = builder.enableImageQuality;
        this.enableFaceAreaLimit = builder.enableFaceAreaLimit;
        this.keepMaxFace = builder.keepMaxFace;
        this.recognizeFailedRetryInterval = builder.recognizeFailedRetryInterval;
        this.livenessParam = builder.livenessParam;
        this.enableFaceSizeLimit = builder.enableFaceSizeLimit;
        this.enableFaceMoveLimit = builder.enableFaceMoveLimit;
        this.faceSizeLimit = builder.faceSizeLimit;
        this.faceMoveLimit = builder.faceMoveLimit;
    }

    public static class Builder {
        private int extractRetryCount = 3;
        private int livenessRetryCount = 3;
        private int maxDetectFaces = 3;
        private int recognizeFailedRetryInterval = 0;
        private int livenessFailedRetryInterval = 0;
        private float similarThreshold = 0.8f;
        private float imageQualityNoMaskRecognizeThreshold = ConfigUtil.IMAGE_QUALITY_NO_MASK_RECOGNIZE_THRESHOLD;
        private float imageQualityMaskRecognizeThreshold = ConfigUtil.IMAGE_QUALITY_MASK_RECOGNIZE_THRESHOLD;
        private boolean enableLiveness = false;
        private boolean enableFaceAreaLimit = false;
        private boolean enableImageQuality = false;
        private boolean enableFaceSizeLimit = false;
        private boolean enableFaceMoveLimit = false;
        private int faceSizeLimit = 0;
        private int faceMoveLimit = 0;
        private boolean keepMaxFace = false;
        private LivenessParam livenessParam;

        public Builder recognizeFailedRetryInterval(int val) {
            this.recognizeFailedRetryInterval = val;
            return this;
        }

        public Builder livenessFailedRetryInterval(int val) {
            this.livenessFailedRetryInterval = val;
            return this;
        }

        public Builder extractRetryCount(int val) {
            this.extractRetryCount = val;
            return this;
        }

        public Builder livenessRetryCount(int val) {
            this.livenessRetryCount = val;
            return this;
        }

        public Builder maxDetectFaces(int val) {
            this.maxDetectFaces = val;
            return this;
        }

        public Builder similarThreshold(float val) {
            this.similarThreshold = val;
            return this;
        }

        public Builder imageQualityNoMaskRecognizeThreshold(float val) {
            this.imageQualityNoMaskRecognizeThreshold = val;
            return this;
        }

        public Builder imageQualityMaskRecognizeThreshold(float val) {
            this.imageQualityMaskRecognizeThreshold = val;
            return this;
        }

        public Builder enableLiveness(boolean val) {
            this.enableLiveness = val;
            return this;
        }


        public Builder enableImageQuality(boolean val) {
            this.enableImageQuality = val;
            return this;
        }
        public Builder enableFaceAreaLimit(boolean val) {
            this.enableFaceAreaLimit = val;
            return this;
        }

        public Builder enableFaceSizeLimit(boolean val) {
            this.enableFaceSizeLimit = val;
            return this;
        }

        public Builder enableFaceMoveLimit(boolean val) {
            this.enableFaceMoveLimit = val;
            return this;
        }

        public Builder faceSizeLimit(int val) {
            this.faceSizeLimit = val;
            return this;
        }

        public Builder faceMoveLimit(int val) {
            this.faceMoveLimit = val;
            return this;
        }

        public Builder keepMaxFace(boolean val) {
            this.keepMaxFace = val;
            return this;
        }

        public Builder livenessParam(LivenessParam val) {
            this.livenessParam = val;
            return this;
        }


        public RecognizeConfiguration build() {
            return new RecognizeConfiguration(this);
        }
    }

    public float getImageQualityNoMaskRecognizeThreshold() {
        return imageQualityNoMaskRecognizeThreshold;
    }

    public float getImageQualityMaskRecognizeThreshold() {
        return imageQualityMaskRecognizeThreshold;
    }

    public boolean isEnableImageQuality() {
        return enableImageQuality;
    }

    public boolean isEnableFaceAreaLimit() {
        return enableFaceAreaLimit;
    }

    public int getExtractRetryCount() {
        return extractRetryCount;
    }

    public int getLivenessRetryCount() {
        return livenessRetryCount;
    }

    public int getMaxDetectFaces() {
        return maxDetectFaces;
    }

    public float getSimilarThreshold() {
        return similarThreshold;
    }

    public boolean isEnableLiveness() {
        return enableLiveness;
    }


    public int getRecognizeFailedRetryInterval() {
        return recognizeFailedRetryInterval;
    }

    public int getLivenessFailedRetryInterval() {
        return livenessFailedRetryInterval;
    }

    public boolean isKeepMaxFace() {
        return keepMaxFace;
    }

    public boolean isEnableFaceSizeLimit() {
        return enableFaceSizeLimit;
    }

    public boolean isEnableFaceMoveLimit() {
        return enableFaceMoveLimit;
    }

    public int getFaceSizeLimit() {
        return faceSizeLimit;
    }

    public int getFaceMoveLimit() {
        return faceMoveLimit;
    }

    @Override
    public String toString() {
        return
                "extractRetryCount: " + extractRetryCount + "\r\n" +
                        "similarThreshold: " + similarThreshold + "\r\n" +
                        "recognizeFailedRetryInterval: " + recognizeFailedRetryInterval + "\r\n" +

                        "keepMaxFace: " + keepMaxFace + "\r\n" +
                        "maxDetectFaces: " + maxDetectFaces + "\r\n" +

                        "enableImageQuality: " + enableImageQuality + "\r\n" +
                        "imageQualityNoMaskRecognizeThreshold: " + imageQualityNoMaskRecognizeThreshold + "\r\n" +
                        "imageQualityMaskRecognizeThreshold: " + imageQualityMaskRecognizeThreshold + "\r\n" +

                        "enableLiveness: " + enableLiveness + "\r\n" +
                        "livenessRetryCount: " + livenessRetryCount + "\r\n";

    }
}

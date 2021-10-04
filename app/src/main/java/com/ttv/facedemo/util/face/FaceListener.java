package com.ttv.facedemo.util.face;

import androidx.annotation.Nullable;

import com.ttv.face.FaceFeature;
import com.ttv.face.LivenessInfo;


public interface FaceListener {

    void onFail(Exception e);

    void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, Integer trackId, Integer errorCode);

    void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, Integer trackId, Integer errorCode);
}

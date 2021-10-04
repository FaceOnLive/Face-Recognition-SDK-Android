
package com.ttv.facedemo.ui.callback;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;


public interface OnRegisterFinishedCallback {

    void onRegisterFinished(FacePreviewInfo facePreviewInfo, boolean success);
}
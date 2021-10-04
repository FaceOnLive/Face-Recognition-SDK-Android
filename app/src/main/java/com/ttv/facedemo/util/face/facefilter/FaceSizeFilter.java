package com.ttv.facedemo.util.face.facefilter;

import android.graphics.Rect;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;

import java.util.List;

public class FaceSizeFilter implements FaceRecognizeFilter {
    private int horizontalSize;
    private int verticalSize;

    private static final String TAG = "FaceSizeFilter";

    public FaceSizeFilter(int horizontalSize, int verticalSize) {
        this.horizontalSize = horizontalSize;
        this.verticalSize = verticalSize;
    }

    @Override
    public void filter(List<FacePreviewInfo> facePreviewInfoList) {
        for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
            if (!facePreviewInfo.isQualityPass()) {
                continue;
            }
            if (facePreviewInfo.getFaceInfoRgb() != null) {
                Rect rgbRect = facePreviewInfo.getFaceInfoRgb().getRect();
                Rect irRect = facePreviewInfo.getFaceInfoIr() == null ? null : facePreviewInfo.getFaceInfoIr().getRect();
                boolean rgbRectValid = rgbRect == null || (rgbRect.width() > horizontalSize && rgbRect.height() > verticalSize);
                boolean irRectValid = irRect == null || (irRect.width() > horizontalSize && irRect.height() > verticalSize);
                facePreviewInfo.setQualityPass(rgbRectValid && irRectValid);
            }
        }
    }
}

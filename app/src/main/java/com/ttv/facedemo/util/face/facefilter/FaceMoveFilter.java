package com.ttv.facedemo.util.face.facefilter;

import android.graphics.Rect;

import com.ttv.facedemo.util.face.model.FacePreviewInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class FaceMoveFilter implements FaceRecognizeFilter {
    private static final String TAG = "FaceMoveFilter";
    private Map<Integer, LinkedBlockingDeque<Rect>> facePositionQueueMap = new ConcurrentHashMap<>();
    private static final int CHECK_QUEUE_SIZE = 5;
    private double movePixels;

    public FaceMoveFilter(double movePixels) {
        this.movePixels = movePixels;
    }

    @Override
    public void filter(List<FacePreviewInfo> facePreviewInfoList) {
        clearFacesNotInPreview(facePreviewInfoList);
        for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
            LinkedBlockingDeque<Rect> rectDeque = facePositionQueueMap.get(facePreviewInfo.getTrackId());
            if (rectDeque == null) {
                rectDeque = new LinkedBlockingDeque<>(CHECK_QUEUE_SIZE);
                facePositionQueueMap.put(facePreviewInfo.getTrackId(), rectDeque);
            }
            if (rectDeque.remainingCapacity() == 0) {
                rectDeque.removeLast();
            }
            rectDeque.push(facePreviewInfo.getFaceInfoRgb().getRect());

            if (!facePreviewInfo.isQualityPass()) {
                continue;
            }

            boolean qualityPass = false;
            if (rectDeque.size() == CHECK_QUEUE_SIZE) {
                qualityPass = true;
                Iterator<Rect> iterator = rectDeque.iterator();
                Rect previous = iterator.next();
                while (iterator.hasNext()) {
                    Rect current = iterator.next();
                    double distance = getDistance(current, previous);
                    previous = current;
                    if (distance > movePixels) {
                        qualityPass = false;
                        break;
                    }
                }
            }
            facePreviewInfo.setQualityPass(qualityPass);
        }
    }

    private void clearFacesNotInPreview(List<FacePreviewInfo> facePreviewInfo) {
        Set<Integer> trackIdSet = facePositionQueueMap.keySet();
        for (Integer trackId : trackIdSet) {
            boolean contains = false;
            for (FacePreviewInfo previewInfo : facePreviewInfo) {
                if (previewInfo.getTrackId() == trackId) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                facePositionQueueMap.remove(trackId);
            }
        }
    }

    public static double getDistance(Rect first, Rect second) {
        int firstX = first.centerX();
        int firstY = first.centerY();

        int secondX = second.centerX();
        int secondY = second.centerY();

        int distanceX = secondX - firstX;
        int distanceY = secondY - firstY;

        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }
}

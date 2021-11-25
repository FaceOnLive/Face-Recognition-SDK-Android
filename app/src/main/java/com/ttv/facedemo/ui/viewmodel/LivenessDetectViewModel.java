package com.ttv.facedemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ttv.face.FaceEngine;
import com.ttv.facedemo.TTVFaceApplication;
import com.ttv.facedemo.R;
import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.facedemo.util.FaceRectTransformer;
import com.ttv.facedemo.util.face.FaceHelper;
import com.ttv.facedemo.util.face.constants.LivenessType;
import com.ttv.facedemo.util.face.constants.RecognizeColor;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.facedemo.util.face.model.RecognizeConfiguration;
import com.ttv.facedemo.widget.FaceRectView;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.LivenessParam;
import com.ttv.face.enums.DetectMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LivenessDetectViewModel extends ViewModel {

    private static final String TAG = "LivenessDetectViewModel";

    private FaceHelper faceHelper;
    private Camera.Size previewSize;

    private MutableLiveData<Integer> ftInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> flInitCode = new MutableLiveData<>();

    private int dualCameraHorizontalOffset;
    private int dualCameraVerticalOffset;
    private boolean needUpdateFaceData;
    private ExecutorService livenessExecutor;

    private ConcurrentHashMap<Integer, Integer> rgbLivenessMap;
    private final ReentrantLock livenessDetectLock = new ReentrantLock();

    public void init(boolean canOpenDualCamera) {
        Context context = TTVFaceApplication.getApplication();
        rgbLivenessMap = new ConcurrentHashMap<>();

        dualCameraHorizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
        dualCameraVerticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
        if (dualCameraHorizontalOffset != 0 || dualCameraVerticalOffset != 0) {
            needUpdateFaceData = true;
        }
        livenessExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("flThread-" + t.getId());
                    return t;
                });
    }

    private void unInit() {

    }

    public void destroy() {
        if (livenessExecutor != null) {
            livenessExecutor.shutdown();
            livenessExecutor = null;
        }
        unInit();
    }

    public void onRgbCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        initFaceHelper(lastPreviewSize);
    }

    public void setRgbFaceRectTransformer(FaceRectTransformer rgbFaceRectTransformer) {
        faceHelper.setRgbFaceRectTransformer(rgbFaceRectTransformer);
    }

    public List<FacePreviewInfo> onPreviewFrame(byte[] nv21) {
        List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, null, false);
        clearLeftFace(facePreviewInfoList);
        return processLiveness(nv21, null, facePreviewInfoList);
    }

    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Enumeration<Integer> keys = rgbLivenessMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                rgbLivenessMap.remove(key);
            }
        }
    }

    private List<FacePreviewInfo> processLiveness(byte[] nv21, byte[] irNv21, List<FacePreviewInfo> previewInfoList) {
        if (previewInfoList == null || previewInfoList.size() == 0) {
            return null;
        }
        if (!livenessDetectLock.isLocked() && livenessExecutor != null) {
            livenessExecutor.execute(() -> {
                List<FacePreviewInfo> facePreviewInfoList = new LinkedList<>(previewInfoList);
                livenessDetectLock.lock();
                try {
                    Context context = TTVFaceApplication.getApplication();

                    if (facePreviewInfoList.isEmpty()) {
                        Log.e(TAG, "facePreviewInfoList isEmpty");
                    } else {

                        FaceEngine.getInstance(context).livenessProcess(nv21, previewSize.width, previewSize.height,
                                new ArrayList<>(Collections.singletonList(facePreviewInfoList.get(0).getFaceInfoRgb())));

                        rgbLivenessMap.put(facePreviewInfoList.get(0).getTrackId(), facePreviewInfoList.get(0).getFaceInfoRgb().liveness);
                    }
                } finally {
                    livenessDetectLock.unlock();
                }
            });
        }
        for (FacePreviewInfo facePreviewInfo : previewInfoList) {
            Integer rgbLiveness = rgbLivenessMap.get(facePreviewInfo.getTrackId());
            if (rgbLiveness != null) {
                facePreviewInfo.setRgbLiveness(rgbLiveness);
            }
        }
        return previewInfoList;
    }
    private void initFaceHelper(Camera.Size lastPreviewSize) {
        if (faceHelper == null ||
                lastPreviewSize == null ||
                lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
            Integer trackedFaceCount = null;
            if (faceHelper != null) {
                trackedFaceCount = faceHelper.getTrackedFaceCount();
                faceHelper.release();
            }
            Context context = TTVFaceApplication.getApplication().getApplicationContext();

            faceHelper = new FaceHelper.Builder()
                    .faceEngine(FaceEngine.getInstance(context))
                    .previewSize(previewSize)
                    .onlyDetectLiveness(true)
                    .recognizeConfiguration(new RecognizeConfiguration.Builder().keepMaxFace(true).build())
                    .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(context) : trackedFaceCount)
                    .build();
        }
    }

    public List<FaceRectView.DrawInfo> getDrawInfo(List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int liveness = facePreviewInfoList.get(i).getRgbLiveness();
            Rect rect = livenessType == LivenessType.RGB ?
                    facePreviewInfoList.get(i).getRgbTransformedRect() :
                    facePreviewInfoList.get(i).getIrTransformedRect();

            int color;
            String name;
            switch (liveness) {
                case LivenessInfo.ALIVE:
                    color = RecognizeColor.COLOR_SUCCESS;
                    name = "REAL";
                    break;
                case LivenessInfo.NOT_ALIVE:
                    color = RecognizeColor.COLOR_FAILED;
                    name = "FAKE";
                    break;
                default:
                    color = RecognizeColor.COLOR_UNKNOWN;
                    name = "";
                    break;
            }

            drawInfoList.add(new FaceRectView.DrawInfo(rect, GenderInfo.UNKNOWN,
                    AgeInfo.UNKNOWN_AGE, liveness, color, name));
        }
        return drawInfoList;
    }

    public Point loadPreviewSize() {
        String[] size = ConfigUtil.getPreviewSize(TTVFaceApplication.getApplication()).split("x");
        return new Point(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
    }
}

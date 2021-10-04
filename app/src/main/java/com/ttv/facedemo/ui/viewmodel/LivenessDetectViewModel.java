package com.ttv.facedemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceInfo;
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

    private FaceSDK flEngine;
    private FaceSDK ftEngine;

    private FaceHelper faceHelper;
    private byte[] irNv21;
    private Camera.Size previewSize;

    private MutableLiveData<Integer> ftInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> flInitCode = new MutableLiveData<>();

    private int dualCameraHorizontalOffset;
    private int dualCameraVerticalOffset;
    private int livenessMask;
    private boolean needUpdateFaceData;
    private ExecutorService livenessExecutor;

    private ConcurrentHashMap<Integer, Integer> rgbLivenessMap;
    private ConcurrentHashMap<Integer, Integer> irLivenessMap;
    private final ReentrantLock livenessDetectLock = new ReentrantLock();

    public void init(boolean canOpenDualCamera) {
        Context context = TTVFaceApplication.getApplication();
        String livenessTypeStr = ConfigUtil.getLivenessDetectType(TTVFaceApplication.getApplication());
        LivenessType livenessType;
        if (livenessTypeStr.equals(TTVFaceApplication.getApplication().getString(R.string.value_liveness_type_ir))) {
            livenessType = LivenessType.IR;
        } else {
            livenessType = LivenessType.RGB;
        }
        rgbLivenessMap = new ConcurrentHashMap<>();
        if (canOpenDualCamera && livenessType == LivenessType.IR) {
            irLivenessMap = new ConcurrentHashMap<>();
            livenessMask = FaceSDK.TTV_LIVENESS | FaceSDK.TTV_IR_LIVENESS | FaceSDK.TTV_FACE_DETECT;
        } else {
            livenessMask = FaceSDK.TTV_LIVENESS;
        }

        dualCameraHorizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
        dualCameraVerticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
        if (dualCameraHorizontalOffset != 0 || dualCameraVerticalOffset != 0) {
            needUpdateFaceData = true;
            livenessMask |= FaceSDK.TTV_UPDATE_FACEDATA;
        }
        LivenessParam livenessParam = new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context), ConfigUtil.getLivenessFqThreshold(context));

        ftEngine = new FaceSDK(context);
        ftInitCode.postValue(ftEngine.init(context, DetectMode.TTV_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), FaceSDK.TTV_FACE_DETECT));

        flEngine = new FaceSDK(context);
        flInitCode.postValue(flEngine.init(context, DetectMode.TTV_DETECT_MODE_IMAGE, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), livenessMask));
        flEngine.setLivenessParam(livenessParam);

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
        if (ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (flEngine != null) {
            synchronized (flEngine) {
                int frUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
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

    public void setIrFaceRectTransformer(FaceRectTransformer irFaceRectTransformer) {
        faceHelper.setIrFaceRectTransformer(irFaceRectTransformer);
    }

    public List<FacePreviewInfo> onPreviewFrame(byte[] nv21) {
        List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, irNv21, false);
        clearLeftFace(facePreviewInfoList);
        return processLiveness(nv21, irNv21, facePreviewInfoList);
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
                if (irLivenessMap != null) {
                    irLivenessMap.remove(key);
                }
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
                    int processRgbLivenessCode;
                    if (facePreviewInfoList.isEmpty()) {
                        Log.e(TAG, "facePreviewInfoList isEmpty");
                    } else {
                        synchronized (flEngine) {
                            processRgbLivenessCode = flEngine.process(nv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21,
                                    new ArrayList<>(Collections.singletonList(facePreviewInfoList.get(0).getFaceInfoRgb())), FaceSDK.TTV_LIVENESS);
                        }
                        if (processRgbLivenessCode != ErrorInfo.MOK) {
                            Log.e(TAG, "process RGB Liveness error: " + processRgbLivenessCode);
                        } else {
                            List<LivenessInfo> rgbLivenessInfoList = new ArrayList<>();
                            int getRgbLivenessCode = flEngine.getLiveness(rgbLivenessInfoList);
                            if (getRgbLivenessCode != ErrorInfo.MOK) {
                                Log.e(TAG, "get RGB LivenessResult error: " + getRgbLivenessCode);
                            } else {
                                rgbLivenessMap.put(facePreviewInfoList.get(0).getTrackId(), rgbLivenessInfoList.get(0).getLiveness());
                            }
                        }
                        if ((livenessMask & FaceSDK.TTV_IR_LIVENESS) != 0) {
                            List<FaceInfo> faceInfoList = new ArrayList<>();
                            FaceInfo irFaceInfo = facePreviewInfoList.get(0).getFaceInfoIr();
                            int fdCode = flEngine.detectFaces(irNv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21, faceInfoList);
                            if (fdCode == ErrorInfo.MOK && FaceHelper.isFaceExists(faceInfoList, irFaceInfo)) {
                                if (needUpdateFaceData) {

                                    int faceDataCode = flEngine.updateFaceData(irNv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21,
                                            new ArrayList<>(Collections.singletonList(irFaceInfo)));
                                    if (faceDataCode != ErrorInfo.MOK) {
                                        Log.e(TAG, "process IR faceData error: " + faceDataCode);
                                    } else {
                                        processIrLive(irFaceInfo, facePreviewInfoList.get(0).getTrackId());
                                    }
                                } else {
                                    processIrLive(irFaceInfo, facePreviewInfoList.get(0).getTrackId());
                                }
                            } else {
                                Log.e(TAG, "process IR Liveness error: " + fdCode);
                            }
                        }
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
            if (irLivenessMap != null) {
                Integer irLiveness = irLivenessMap.get(facePreviewInfo.getTrackId());
                if (irLiveness != null) {
                    facePreviewInfo.setIrLiveness(irLiveness);
                }
            }
        }
        return previewInfoList;
    }

    private void processIrLive(FaceInfo irFaceInfo, int trackId) {
        int processIrLivenessCode;
        synchronized (flEngine) {
            processIrLivenessCode = flEngine.processIr(irNv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21,
                    Arrays.asList(irFaceInfo), FaceSDK.TTV_IR_LIVENESS);
        }
        if (processIrLivenessCode != ErrorInfo.MOK) {
            Log.e(TAG, "process IR Liveness error: " + processIrLivenessCode);
        } else {
            List<LivenessInfo> irLivenessInfoList = new ArrayList<>();
            int getIrLivenessCode = flEngine.getIrLiveness(irLivenessInfoList);
            if (getIrLivenessCode != ErrorInfo.MOK) {
                Log.e(TAG, "get IR LivenessResult error: " + getIrLivenessCode);
            } else {
                irLivenessMap.put(trackId, irLivenessInfoList.get(0).getLiveness());
            }
        }
    }

    public void onIrCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        initFaceHelper(lastPreviewSize);
        faceHelper.setDualCameraFaceInfoTransformer(faceInfo -> {
            FaceInfo irFaceInfo = new FaceInfo(faceInfo);
            irFaceInfo.getRect().offset(dualCameraHorizontalOffset, dualCameraVerticalOffset);
            return irFaceInfo;
        });
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
                    .ftEngine(ftEngine)
                    .previewSize(previewSize)
                    .onlyDetectLiveness(true)
                    .recognizeConfiguration(new RecognizeConfiguration.Builder().keepMaxFace(true).build())
                    .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(context) : trackedFaceCount)
                    .build();
        }
    }

    public void refreshIrPreviewData(byte[] nv21) {
        irNv21 = nv21;
    }

    public List<FaceRectView.DrawInfo> getDrawInfo(List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int liveness = livenessType == LivenessType.RGB ? facePreviewInfoList.get(i).getRgbLiveness() : facePreviewInfoList.get(i).getIrLiveness();
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

package com.ttv.facedemo.util.face;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttv.facedemo.facedb.FaceManager;
import com.ttv.facedemo.ui.model.CompareResult;
import com.ttv.facedemo.util.FaceRectTransformer;
import com.ttv.facedemo.util.face.constants.LivenessType;
import com.ttv.facedemo.util.face.constants.RequestFeatureStatus;
import com.ttv.facedemo.util.face.constants.RequestLivenessStatus;
import com.ttv.facedemo.util.face.facefilter.FaceMoveFilter;
import com.ttv.facedemo.util.face.facefilter.FaceRecognizeAreaFilter;
import com.ttv.facedemo.util.face.facefilter.FaceRecognizeFilter;
import com.ttv.facedemo.util.face.facefilter.FaceSizeFilter;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.facedemo.util.face.model.RecognizeConfiguration;
import com.ttv.facedemo.util.face.model.RecognizeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceFeature;
import com.ttv.face.FaceInfo;
import com.ttv.face.ImageQualitySimilar;
import com.ttv.face.LivenessInfo;
import com.ttv.face.MaskInfo;
import com.ttv.face.enums.ExtractType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class FaceHelper implements FaceListener {

    private static final String TAG = "FaceHelper";

    private RecognizeCallback recognizeCallback;


    private ConcurrentHashMap<Integer, RecognizeInfo> recognizeInfoMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    private IDualCameraFaceInfoTransformer dualCameraFaceInfoTransformer;

    private static final int ERROR_BUSY = -1;
    private static final int ERROR_FR_ENGINE_IS_NULL = -2;
    private static final int ERROR_FL_ENGINE_IS_NULL = -3;
    private FaceSDK ftEngine;
    private FaceSDK frEngine;
    private FaceSDK flEngine;

    private Camera.Size previewSize;

    private List<FaceInfo> faceInfoList = new CopyOnWriteArrayList<>();
    private List<MaskInfo> maskInfoList = new CopyOnWriteArrayList<>();
    private ExecutorService frExecutor;
    private ExecutorService flExecutor;
    private LinkedBlockingQueue<Runnable> frThreadQueue;
    private LinkedBlockingQueue<Runnable> flThreadQueue;

    private FaceRectTransformer rgbFaceRectTransformer;
    private FaceRectTransformer irFaceRectTransformer;
    private Rect recognizeArea = new Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private List<FaceRecognizeFilter> faceRecognizeFilterList = new ArrayList<>();
    private int trackedFaceCount = 0;
    private int currentMaxFaceId = 0;

    private boolean onlyDetectLiveness;
    private boolean needUpdateFaceData;
    private RecognizeConfiguration recognizeConfiguration;
    private List<Integer> currentTrackIdList = new ArrayList<>();
    private List<FacePreviewInfo> facePreviewInfoList = new ArrayList<>();
    private Disposable timerDisposable;

    private FaceHelper(Builder builder) {
        needUpdateFaceData = builder.needUpdateFaceData;
        onlyDetectLiveness = builder.onlyDetectLiveness;
        ftEngine = builder.ftEngine;
        trackedFaceCount = builder.trackedFaceCount;
        previewSize = builder.previewSize;
        frEngine = builder.frEngine;
        flEngine = builder.flEngine;
        recognizeCallback = builder.recognizeCallback;
        recognizeConfiguration = builder.recognizeConfiguration;
        dualCameraFaceInfoTransformer = builder.dualCameraFaceInfoTransformer;

        int frQueueSize = recognizeConfiguration.getMaxDetectFaces();
        if (builder.frQueueSize > 0) {
            frQueueSize = builder.frQueueSize;
        } else {
            Log.e(TAG, "frThread num must > 0, now using default value:" + frQueueSize);
        }
        frThreadQueue = new LinkedBlockingQueue<>(frQueueSize);
        frExecutor = new ThreadPoolExecutor(1, frQueueSize, 0, TimeUnit.MILLISECONDS, frThreadQueue, r -> {
            Thread t = new Thread(r);
            t.setName("frThread-" + t.getId());
            return t;
        });

        int flQueueSize = recognizeConfiguration.getMaxDetectFaces();
        if (builder.flQueueSize > 0) {
            flQueueSize = builder.flQueueSize;
        } else {
            Log.e(TAG, "flThread num must > 0, now using default value:" + flQueueSize);
        }
        flThreadQueue = new LinkedBlockingQueue<Runnable>(flQueueSize);
        flExecutor = new ThreadPoolExecutor(1, flQueueSize, 0, TimeUnit.MILLISECONDS, flThreadQueue, r -> {
            Thread t = new Thread(r);
            t.setName("flThread-" + t.getId());
            return t;
        });
        if (previewSize == null) {
            throw new RuntimeException("previewSize must be specified!");
        }
        if (recognizeConfiguration.isEnableFaceSizeLimit()) {
            faceRecognizeFilterList.add(new FaceSizeFilter(recognizeConfiguration.getFaceSizeLimit(), recognizeConfiguration.getFaceSizeLimit()));
        }
        if (recognizeConfiguration.isEnableFaceMoveLimit()) {
            faceRecognizeFilterList.add(new FaceMoveFilter(recognizeConfiguration.getFaceMoveLimit()));
        }
        if (recognizeConfiguration.isEnableFaceAreaLimit()) {
            faceRecognizeFilterList.add(new FaceRecognizeAreaFilter(recognizeArea));
        }
    }

    public void requestFaceFeature(byte[] nv21, FacePreviewInfo facePreviewInfo, int width, int height, int format) {
        if (frEngine != null && frThreadQueue.remainingCapacity() > 0) {
            frExecutor.execute(new FaceRecognizeRunnable(nv21, facePreviewInfo, width, height, format));
        } else {
            onFaceFeatureInfoGet(null, facePreviewInfo.getTrackId(), ERROR_BUSY);
        }
    }

    public void requestFaceLiveness(byte[] nv21, FacePreviewInfo faceInfo, int width, int height, int format, LivenessType livenessType, Object waitLock) {
        if (flEngine != null && flThreadQueue.remainingCapacity() > 0) {
            flExecutor.execute(new FaceLivenessDetectRunnable(nv21, faceInfo, width, height, format, livenessType, waitLock));
        } else {
            onFaceLivenessInfoGet(null, faceInfo.getTrackId(), ERROR_BUSY);
        }

    }

    public void release() {
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (!frExecutor.isShutdown()) {
            frExecutor.shutdownNow();
            frThreadQueue.clear();
        }
        if (!flExecutor.isShutdown()) {
            flExecutor.shutdownNow();
            flThreadQueue.clear();
        }
        if (faceInfoList != null) {
            faceInfoList.clear();
        }
        if (frThreadQueue != null) {
            frThreadQueue.clear();
            frThreadQueue = null;
        }
        if (flThreadQueue != null) {
            flThreadQueue.clear();
            flThreadQueue = null;
        }
        faceInfoList = null;
    }

    public List<FacePreviewInfo> onPreviewFrame(@NonNull byte[] rgbNv21, @Nullable byte[] irNv21, boolean doRecognize) {
        if (ftEngine != null) {
            faceInfoList.clear();
            maskInfoList.clear();
            facePreviewInfoList.clear();
            int code = ftEngine.detectFaces(rgbNv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21, faceInfoList);
            if (code != ErrorInfo.MOK) {
                onFail(new Exception("detectFaces failed,code is " + code));
                return facePreviewInfoList;
            }
            if (recognizeConfiguration.isKeepMaxFace()) {
                keepMaxFace(faceInfoList);
            }
            refreshTrackId(faceInfoList);
            if (faceInfoList.isEmpty()) {
                return facePreviewInfoList;
            }
            if (!onlyDetectLiveness) {
                code = ftEngine.process(rgbNv21, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21, faceInfoList,
                        FaceSDK.TTV_MASK_DETECT);
                if (code == ErrorInfo.MOK) {
                    code = ftEngine.getMask(maskInfoList);
                    if (code != ErrorInfo.MOK) {
                        onFail(new Exception("process getMask failed,code is " + code));
                        return facePreviewInfoList;
                    }
                } else {
                    onFail(new Exception("process mask failed,code is " + code));
                    return facePreviewInfoList;
                }
            }

            for (int i = 0; i < faceInfoList.size(); i++) {
                FacePreviewInfo facePreviewInfo = new FacePreviewInfo(faceInfoList.get(i), currentTrackIdList.get(i));
                if (!maskInfoList.isEmpty()) {
                    MaskInfo maskInfo = maskInfoList.get(i);
                    facePreviewInfo.setMask(maskInfo.getMask());
                }
                if (rgbFaceRectTransformer != null && recognizeArea != null) {
                    Rect rect = rgbFaceRectTransformer.adjustRect(faceInfoList.get(i).getRect());
                    Rect foreRect = rgbFaceRectTransformer.adjustRect(faceInfoList.get(i).getForeheadRect());
                    facePreviewInfo.setRgbTransformedRect(rect);
                    facePreviewInfo.setForeRect(foreRect);
                }
                if (irFaceRectTransformer != null) {
                    FaceInfo faceInfo = faceInfoList.get(i);
                    if (dualCameraFaceInfoTransformer != null) {
                        faceInfo = dualCameraFaceInfoTransformer.transformFaceInfo(faceInfo);
                    }
                    facePreviewInfo.setFaceInfoIr(faceInfo);
                    facePreviewInfo.setIrTransformedRect(irFaceRectTransformer.adjustRect(faceInfo.getRect()));
                }
                facePreviewInfoList.add(facePreviewInfo);
            }
            clearLeftFace(facePreviewInfoList);
            if (doRecognize) {
                doRecognize(rgbNv21, irNv21, facePreviewInfoList);
            }
        } else {
            facePreviewInfoList.clear();
        }
        return facePreviewInfoList;
    }

    public void setRgbFaceRectTransformer(FaceRectTransformer rgbFaceRectTransformer) {
        this.rgbFaceRectTransformer = rgbFaceRectTransformer;
    }

    public void setIrFaceRectTransformer(FaceRectTransformer irFaceRectTransformer) {
        this.irFaceRectTransformer = irFaceRectTransformer;
    }

    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
        }
        Enumeration<Integer> keys = recognizeInfoMap.keys();
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
                RecognizeInfo recognizeInfo = recognizeInfoMap.remove(key);
                if (recognizeInfo != null) {
                    recognizeCallback.onNoticeChanged("");
                    synchronized (recognizeInfo.getWaitLock()) {
                        recognizeInfo.getWaitLock().notifyAll();
                    }
                }
            }
        }
    }

    private void doRecognize(byte[] rgbNv21, byte[] irNv21, List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList != null && !facePreviewInfoList.isEmpty() && previewSize != null) {
            for (FaceRecognizeFilter faceRecognizeFilter : faceRecognizeFilterList) {
                faceRecognizeFilter.filter(facePreviewInfoList);
            }
            for (int i = 0; i < facePreviewInfoList.size(); i++) {
                FacePreviewInfo facePreviewInfo = facePreviewInfoList.get(i);
                if (!facePreviewInfo.isQualityPass()) {
                    continue;
                }
                if (!onlyDetectLiveness && facePreviewInfo.getMask() == MaskInfo.UNKNOWN) {
                    continue;
                }
                RecognizeInfo recognizeInfo = getRecognizeInfo(recognizeInfoMap, facePreviewInfo.getTrackId());
                int status = recognizeInfo.getRecognizeStatus();

                if (recognizeConfiguration.isEnableLiveness() && status != RequestFeatureStatus.SUCCEED) {
                    int liveness = recognizeInfo.getLiveness();
                    if (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING
                            || status == RequestFeatureStatus.FAILED) {
                        changeLiveness(facePreviewInfo.getTrackId(), RequestLivenessStatus.ANALYZING);
                        requestFaceLiveness(
                                irNv21 == null ? rgbNv21 : irNv21,
                                facePreviewInfo,
                                previewSize.width,
                                previewSize.height,
                                FaceSDK.CP_PAF_NV21,
                                irNv21 == null ? LivenessType.RGB : LivenessType.IR,
                                recognizeInfo.getWaitLock()
                        );
                    }
                }

                if (status == RequestFeatureStatus.TO_RETRY) {
                    changeRecognizeStatus(facePreviewInfo.getTrackId(), RequestFeatureStatus.SEARCHING);
                    requestFaceFeature(
                            rgbNv21, facePreviewInfo,
                            previewSize.width,
                            previewSize.height,
                            FaceSDK.CP_PAF_NV21
                    );
                }
            }
        }
    }

    @Override
    public void onFail(Exception e) {
        Log.e(TAG, "onFail:" + e.getMessage());
    }

    public RecognizeInfo getRecognizeInfo(Map<Integer, RecognizeInfo> recognizeInfoMap, int trackId) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        if (recognizeInfo == null) {
            recognizeInfo = new RecognizeInfo();
            recognizeInfoMap.put(trackId, recognizeInfo);
        }
        return recognizeInfo;
    }

    @Override
    public void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, Integer trackId, Integer errorCode) {

        RecognizeInfo recognizeInfo = getRecognizeInfo(recognizeInfoMap, trackId);
        if (faceFeature != null) {

            if (recognizeInfo == null) {
                return;
            }

            if (!recognizeConfiguration.isEnableLiveness()) {
                searchFace(faceFeature, trackId);
            }

            else if (recognizeInfo.getLiveness() == LivenessInfo.ALIVE) {
                searchFace(faceFeature, trackId);
            }

            else {
                synchronized (recognizeInfo.getWaitLock()) {
                    try {
                        recognizeInfo.getWaitLock().wait();
                        if (recognizeInfoMap.containsKey(trackId)) {
                            onFaceFeatureInfoGet(faceFeature, trackId, errorCode);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        else {
            if (recognizeInfo.increaseAndGetExtractErrorRetryCount() > recognizeConfiguration.getExtractRetryCount()) {
                recognizeInfo.setExtractErrorRetryCount(0);
                retryRecognizeDelayed(trackId);
            } else {
                changeRecognizeStatus(trackId, RequestFeatureStatus.TO_RETRY);
            }
        }
    }

    private void retryLivenessDetectDelayed(final Integer trackId) {
        Observable.timer(recognizeConfiguration.getLivenessFailedRetryInterval(), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        changeLiveness(trackId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    private void retryRecognizeDelayed(final Integer trackId) {
        changeRecognizeStatus(trackId, RequestFeatureStatus.FAILED);
        Observable.timer(recognizeConfiguration.getRecognizeFailedRetryInterval(), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        changeRecognizeStatus(trackId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    @Override
    public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, Integer trackId, Integer errorCode) {
        if (livenessInfo != null) {
            int liveness = livenessInfo.getLiveness();
            Log.i(TAG, "onFaceLivenessInfoGet liveness:" + liveness);
            changeLiveness(trackId, liveness);
            // 非活体，重试
            if (liveness != LivenessInfo.ALIVE) {
                noticeCurrentStatus("Liveness failed.");
                retryLivenessDetectDelayed(trackId);
            }
        } else {
            RecognizeInfo recognizeInfo = getRecognizeInfo(recognizeInfoMap, trackId);
            if (recognizeInfo.increaseAndGetLivenessErrorRetryCount() > recognizeConfiguration.getLivenessRetryCount()) {
                recognizeInfo.setLivenessErrorRetryCount(0);
                retryLivenessDetectDelayed(trackId);
            } else {
                changeLiveness(trackId, LivenessInfo.UNKNOWN);
            }
        }
    }

    private void noticeCurrentStatus(String notice) {
        if (recognizeCallback != null) {
            recognizeCallback.onNoticeChanged(notice);
        }
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
        timerDisposable = Observable.timer(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (recognizeCallback != null) {
                        recognizeCallback.onNoticeChanged("");
                    }
                });
    }

    private void searchFace(final FaceFeature faceFeature, final Integer trackId) {
        CompareResult compareResult = FaceManager.getInstance().searchFaceFeature(faceFeature, frEngine);
        if (compareResult == null || compareResult.getFaceEntity() == null) {
            retryRecognizeDelayed(trackId);
            return;
        }
        compareResult.setTrackId(trackId);
        boolean pass = compareResult.getSimilar() > recognizeConfiguration.getSimilarThreshold();
        recognizeCallback.onRecognized(compareResult, getRecognizeInfo(recognizeInfoMap, trackId).getLiveness(), pass);
        if (pass) {
            setName(trackId, "");
            noticeCurrentStatus("Recognized");
            changeRecognizeStatus(trackId, RequestFeatureStatus.SUCCEED);
        } else {
            noticeCurrentStatus("Failed：NOT_REGISTERED");
            retryRecognizeDelayed(trackId);
        }
    }

    public class FaceRecognizeRunnable implements Runnable {
        private FaceInfo faceInfo;
        private int width;
        private int height;
        private int format;
        private Integer trackId;
        private byte[] nv21Data;
        private int isMask;

        private FaceRecognizeRunnable(byte[] nv21Data, FacePreviewInfo facePreviewInfo, int width, int height, int format) {
            if (nv21Data == null) {
                return;
            }
            this.nv21Data = nv21Data;
            this.faceInfo = new FaceInfo(facePreviewInfo.getFaceInfoRgb());
            this.width = width;
            this.height = height;
            this.format = format;
            this.trackId = facePreviewInfo.getTrackId();
            this.isMask = facePreviewInfo.getMask();
        }

        @Override
        public void run() {
            if (nv21Data != null) {
                if (frEngine != null) {
                    if (recognizeConfiguration.isEnableImageQuality()) {

                        ImageQualitySimilar qualitySimilar = new ImageQualitySimilar();
                        int iqCode;
                        long iqStartTime = System.currentTimeMillis();
                        synchronized (frEngine) {
                            iqCode = frEngine.imageQualityDetect(nv21Data, width, height, format, faceInfo, isMask, qualitySimilar);
                        }
                        Log.i(TAG, "fr iqTime:" + (System.currentTimeMillis() - iqStartTime) + "ms");
                        if (iqCode == ErrorInfo.MOK) {
                            float quality = qualitySimilar.getScore();
                            float destQuality = isMask == MaskInfo.WORN ? recognizeConfiguration.getImageQualityMaskRecognizeThreshold() :
                                    recognizeConfiguration.getImageQualityNoMaskRecognizeThreshold();
                            if (quality >= destQuality) {
                                extractFace();
                            } else {
                                onFaceFail(iqCode, "fr imageQualityDetect score invalid");
                            }
                        } else {
                            onFaceFail(iqCode, "fr imageQuality failed errorCode is " + iqCode);
                        }
                    } else {
                        extractFace();
                    }
                } else {
                    onFaceFail(ERROR_FR_ENGINE_IS_NULL, "fr failed ,frEngine is null");
                }
            }
            nv21Data = null;
        }

        private void extractFace() {
            long irStartTime = System.currentTimeMillis();
            FaceFeature faceFeature = new FaceFeature();
            int frCode;
            synchronized (frEngine) {

                frCode = frEngine.extractFaceFeature(nv21Data, width, height, format, faceInfo, ExtractType.RECOGNIZE, isMask, faceFeature);
            }
            Log.i(TAG, "frTime:" + (System.currentTimeMillis() - irStartTime) + "ms");
            if (frCode == ErrorInfo.MOK) {
                onFaceFeatureInfoGet(faceFeature, trackId, frCode);
            } else {
                onFaceFail(frCode, "fr failed errorCode is " + frCode);
            }
        }

        private void onFaceFail(int code, String errorMsg) {
            onFaceFeatureInfoGet(null, trackId, code);
            onFail(new Exception(errorMsg));
        }
    }

    public class FaceLivenessDetectRunnable implements Runnable {
        private FaceInfo faceInfo;
        private int width;
        private int height;
        private int format;
        private Integer trackId;
        private byte[] nv21Data;
        private LivenessType livenessType;
        private Object waitLock;

        private FaceLivenessDetectRunnable(byte[] nv21Data, FacePreviewInfo faceInfo, int width, int height, int format, LivenessType livenessType, Object waitLock) {
            if (nv21Data == null) {
                return;
            }
            this.nv21Data = nv21Data;
            this.faceInfo = new FaceInfo(faceInfo.getFaceInfoRgb());
            this.width = width;
            this.height = height;
            this.format = format;
            this.trackId = faceInfo.getTrackId();
            this.livenessType = livenessType;
            this.waitLock = waitLock;
        }

        @Override
        public void run() {
            if (nv21Data != null) {
                if (flEngine != null) {
                    processLiveness();
                } else {
                    onProcessFail(ERROR_FL_ENGINE_IS_NULL, "fl failed ,frEngine is null");
                }
            }
            nv21Data = null;
        }

        private void processLiveness() {
            List<LivenessInfo> livenessInfoList = new ArrayList<>();
            int flCode = -1;
            synchronized (flEngine) {
                long flStartTime = System.currentTimeMillis();
                if (livenessType == LivenessType.RGB) {

                    flCode = flEngine.process(nv21Data, width, height, format, Arrays.asList(faceInfo), FaceSDK.TTV_LIVENESS);
                } else {
                    if (dualCameraFaceInfoTransformer != null) {
                        faceInfo = dualCameraFaceInfoTransformer.transformFaceInfo(faceInfo);
                    }
                    List<FaceInfo> faceInfoList = new ArrayList<>();
                    int fdCode = flEngine.detectFaces(nv21Data, width, height, format, faceInfoList);
                    boolean isFaceExists = isFaceExists(faceInfoList, faceInfo);
                    if (fdCode == ErrorInfo.MOK && isFaceExists) {
                        if (needUpdateFaceData) {

                            flCode = flEngine.updateFaceData(nv21Data, previewSize.width, previewSize.height, FaceSDK.CP_PAF_NV21,
                                    new ArrayList<>(Collections.singletonList(faceInfo)));
                            if (flCode == ErrorInfo.MOK) {
                                flCode = flEngine.processIr(nv21Data, width, height, format, Arrays.asList(faceInfo), FaceSDK.TTV_IR_LIVENESS);
                            }
                        } else {
                            flCode = flEngine.processIr(nv21Data, width, height, format, Arrays.asList(faceInfo), FaceSDK.TTV_IR_LIVENESS);
                        }
                    } else {
                        onFail(new Exception("ir detectFaces failed fdCode:" + fdCode + ",isFaceExists:" + isFaceExists));
                    }
                }
                Log.i(TAG, "flTime:" + (System.currentTimeMillis() - flStartTime) + "ms");
            }
            if (flCode == ErrorInfo.MOK) {
                if (livenessType == LivenessType.RGB) {
                    flCode = flEngine.getLiveness(livenessInfoList);
                } else {
                    flCode = flEngine.getIrLiveness(livenessInfoList);
                }
            }

            if (flCode == ErrorInfo.MOK && !livenessInfoList.isEmpty()) {
                onFaceLivenessInfoGet(livenessInfoList.get(0), trackId, flCode);
                if (livenessInfoList.get(0).getLiveness() == LivenessInfo.ALIVE) {
                    synchronized (waitLock) {
                        waitLock.notifyAll();
                    }
                }
            } else {
                onProcessFail(flCode, "fl failed errorCode is " + flCode);
            }
        }

        private void onProcessFail(int code, String msg) {
            onFaceLivenessInfoGet(null, trackId, code);
            onFail(new Exception(msg));
        }
    }

    public static boolean isFaceExists(List<FaceInfo> faceInfoList, FaceInfo faceInfo) {
        if (faceInfoList == null || faceInfoList.isEmpty() || faceInfo == null) {
            return false;
        }
        for (FaceInfo info : faceInfoList) {
            if (Rect.intersects(faceInfo.getRect(), info.getRect())) {
                return true;
            }
        }
        return false;
    }


    private void refreshTrackId(List<FaceInfo> ftFaceList) {
        currentTrackIdList.clear();
        for (FaceInfo faceInfo : ftFaceList) {
            currentTrackIdList.add(faceInfo.getFaceId() + trackedFaceCount);
        }
        if (!ftFaceList.isEmpty()) {
            currentMaxFaceId = ftFaceList.get(ftFaceList.size() - 1).getFaceId();
        }
    }
    public int getTrackedFaceCount() {
        // 引擎的人脸下标从0开始，因此需要+1
        return trackedFaceCount + currentMaxFaceId + 1;
    }

    public void setName(int trackId, String name) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        if (recognizeInfo != null) {
            recognizeInfo.setName(name);
        }
    }

    public void setDualCameraFaceInfoTransformer(IDualCameraFaceInfoTransformer transformer) {
        this.dualCameraFaceInfoTransformer = transformer;
    }


    public String getName(int trackId) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        return recognizeInfo == null ? null : recognizeInfo.getName();
    }

    public void setRecognizeArea(Rect recognizeArea) {
        if (recognizeArea != null) {
            this.recognizeArea.set(recognizeArea);
        }
    }

    @IntDef(value = {
            RequestFeatureStatus.FAILED,
            RequestFeatureStatus.SEARCHING,
            RequestFeatureStatus.SUCCEED,
            RequestFeatureStatus.TO_RETRY
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface RequestFaceFeatureStatus {
    }

    @IntDef(value = {
            LivenessInfo.ALIVE,
            LivenessInfo.NOT_ALIVE,
            LivenessInfo.UNKNOWN,
            LivenessInfo.FACE_NUM_MORE_THAN_ONE,
            LivenessInfo.FACE_TOO_SMALL,
            LivenessInfo.FACE_ANGLE_TOO_LARGE,
            LivenessInfo.FACE_BEYOND_BOUNDARY,
            RequestLivenessStatus.ANALYZING
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface RequestFaceLivenessStatus {
    }

    public void changeRecognizeStatus(int trackId, @RequestFaceFeatureStatus int newStatus) {
        getRecognizeInfo(recognizeInfoMap, trackId).setRecognizeStatus(newStatus);
    }

    public void changeLiveness(int trackId, @RequestFaceLivenessStatus int newLiveness) {
        getRecognizeInfo(recognizeInfoMap, trackId).setLiveness(newLiveness);
    }

    public Integer getLiveness(int trackId) {
        return getRecognizeInfo(recognizeInfoMap, trackId).getLiveness();
    }

    public Integer getRecognizeStatus(int trackId) {
        return getRecognizeInfo(recognizeInfoMap, trackId).getRecognizeStatus();
    }

    private static void keepMaxFace(List<FaceInfo> ftFaceList) {
        if (ftFaceList == null || ftFaceList.size() <= 1) {
            return;
        }
        FaceInfo maxFaceInfo = ftFaceList.get(0);
        for (FaceInfo faceInfo : ftFaceList) {
            if (faceInfo.getRect().width() > maxFaceInfo.getRect().width()) {
                maxFaceInfo = faceInfo;
            }
        }
        ftFaceList.clear();
        ftFaceList.add(maxFaceInfo);
    }


    public static final class Builder {
        private FaceSDK ftEngine;
        private FaceSDK frEngine;
        private FaceSDK flEngine;
        private Camera.Size previewSize;
        private boolean onlyDetectLiveness;
        private boolean needUpdateFaceData;
        private RecognizeConfiguration recognizeConfiguration;
        private RecognizeCallback recognizeCallback;
        private IDualCameraFaceInfoTransformer dualCameraFaceInfoTransformer;
        private int frQueueSize;
        private int flQueueSize;
        private int trackedFaceCount;

        public Builder() {
        }

        public Builder recognizeConfiguration(RecognizeConfiguration val) {
            recognizeConfiguration = val;
            return this;
        }

        public Builder dualCameraFaceInfoTransformer(IDualCameraFaceInfoTransformer val) {
            dualCameraFaceInfoTransformer = val;
            return this;
        }

        public Builder recognizeCallback(RecognizeCallback val) {
            recognizeCallback = val;
            return this;
        }

        public Builder ftEngine(FaceSDK val) {
            ftEngine = val;
            return this;
        }

        public Builder frEngine(FaceSDK val) {
            frEngine = val;
            return this;
        }

        public Builder flEngine(FaceSDK val) {
            flEngine = val;
            return this;
        }

        public Builder previewSize(Camera.Size val) {
            previewSize = val;
            return this;
        }

        public Builder frQueueSize(int val) {
            frQueueSize = val;
            return this;
        }

        public Builder flQueueSize(int val) {
            flQueueSize = val;
            return this;
        }

        public Builder trackedFaceCount(int val) {
            trackedFaceCount = val;
            return this;
        }

        public Builder onlyDetectLiveness(boolean val) {
            onlyDetectLiveness = val;
            return this;
        }

        public Builder needUpdateFaceData(boolean val) {
            needUpdateFaceData = val;
            return this;
        }

        public FaceHelper build() {
            return new FaceHelper(this);
        }
    }
}

package com.ttv.facedemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ttv.facedemo.TTVFaceApplication;
import com.ttv.facedemo.R;
import com.ttv.facedemo.ui.model.CompareResult;
import com.ttv.facedemo.facedb.FaceManager;
import com.ttv.facedemo.ui.callback.OnRegisterFinishedCallback;
import com.ttv.facedemo.ui.model.PreviewConfig;
import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.facedemo.util.FaceRectTransformer;
import com.ttv.facedemo.util.face.FaceHelper;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.facedemo.util.face.constants.LivenessType;
import com.ttv.facedemo.util.face.RecognizeCallback;
import com.ttv.facedemo.util.face.constants.RecognizeColor;
import com.ttv.facedemo.util.face.model.RecognizeConfiguration;
import com.ttv.facedemo.util.face.constants.RequestFeatureStatus;
import com.ttv.facedemo.widget.FaceRectView;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceAttributeParam;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceInfo;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.LivenessParam;
import com.ttv.face.MaskInfo;
import com.ttv.face.enums.DetectFaceOrientPriority;
import com.ttv.face.enums.DetectMode;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RecognizeViewModel extends ViewModel implements RecognizeCallback {

    public enum EventType {

        INSERTED,

        REMOVED
    }

    public static class FaceItemEvent {
        private int index;
        private EventType eventType;

        public FaceItemEvent(int index, EventType eventType) {
            this.index = index;
            this.eventType = eventType;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }
    }

    private static final String TAG = "RecognizeViewModel";


    private OnRegisterFinishedCallback onRegisterFinishedCallback;

    public static final int REGISTER_STATUS_READY = 0;

    public static final int REGISTER_STATUS_PROCESSING = 1;

    public static final int REGISTER_STATUS_DONE = 2;


    private int registerStatus = REGISTER_STATUS_DONE;

    private static final int MAX_DETECT_NUM = 10;

    private Camera.Size previewSize;

    private MutableLiveData<List<CompareResult>> compareResultList;

    private MutableLiveData<FaceItemEvent> faceItemEventMutableLiveData = new MutableLiveData<>();

    private MutableLiveData<Integer> ftInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> frInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> flInitCode = new MutableLiveData<>();

    private FaceHelper faceHelper;

    private FaceSDK ftEngine;

    private FaceSDK frEngine;

    private FaceSDK flEngine;

    private PreviewConfig previewConfig;

    private MutableLiveData<RecognizeConfiguration> recognizeConfiguration = new MutableLiveData<>();

    private MutableLiveData<String> recognizeNotice = new MutableLiveData<>();

    private MutableLiveData<String> drawRectInfoText = new MutableLiveData<>();

    private boolean needUpdateFaceData;

    private LivenessType livenessType;

    private byte[] irNV21 = null;

    private boolean loadFaceList;

    private Disposable registerNv21Disposable;

    public void refreshIrPreviewData(byte[] irPreviewData) {
        irNV21 = irPreviewData;
    }

    public void setLiveType(LivenessType liveType) {
        this.livenessType = liveType;
    }

    public void setRgbFaceRectTransformer(FaceRectTransformer rgbFaceRectTransformer) {
        faceHelper.setRgbFaceRectTransformer(rgbFaceRectTransformer);
    }

    public void setIrFaceRectTransformer(FaceRectTransformer irFaceRectTransformer) {
        faceHelper.setIrFaceRectTransformer(irFaceRectTransformer);
    }

    private void registerFace(final byte[] nv21, FacePreviewInfo facePreviewInfo) {
        updateRegisterStatus(REGISTER_STATUS_PROCESSING);
        registerNv21Disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            FaceSDK registerEngine = new FaceSDK(TTVFaceApplication.getApplication());
            int res = registerEngine.init(TTVFaceApplication.getApplication(), DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_0_ONLY,
                    1, FaceSDK.TTV_FACE_RECOGNITION);
            if (res == ErrorInfo.MOK) {
                boolean success = FaceManager.getInstance().registerNv21(TTVFaceApplication.getApplication(), nv21.clone(), previewSize.width,
                        previewSize.height, facePreviewInfo, "registered_" + faceHelper.getTrackedFaceCount(), frEngine, registerEngine);
                registerEngine.unInit();
                emitter.onNext(success);
            } else {
                emitter.onNext(false);
            }
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Boolean>() {

                    @Override
                    public void onNext(Boolean success) {
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, success);
                        }
                        updateRegisterStatus(REGISTER_STATUS_DONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, false);
                        }
                        updateRegisterStatus(REGISTER_STATUS_DONE);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

    }

    public MutableLiveData<List<CompareResult>> getCompareResultList() {
        if (compareResultList == null) {
            compareResultList = new MutableLiveData<>();
            compareResultList.setValue(new ArrayList<>());
        }
        return compareResultList;
    }

    public void init() {
        Context context = TTVFaceApplication.getApplication();
        boolean switchCamera = ConfigUtil.isSwitchCamera(context);
        previewConfig = new PreviewConfig(
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK,
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT,
                Integer.parseInt(ConfigUtil.getRgbCameraAdditionalRotation(context)),
                Integer.parseInt(ConfigUtil.getIrCameraAdditionalRotation(context))
        );

        boolean enableLive = !ConfigUtil.getLivenessDetectType(context).equals(context.getString(R.string.value_liveness_type_disable));
        boolean enableFaceQualityDetect = ConfigUtil.isEnableImageQualityDetect(context);
        boolean enableFaceMoveLimit = ConfigUtil.isEnableFaceMoveLimit(context);
        boolean enableFaceSizeLimit = ConfigUtil.isEnableFaceSizeLimit(context);
        RecognizeConfiguration configuration = new RecognizeConfiguration.Builder()
                .enableFaceMoveLimit(enableFaceMoveLimit)
                .enableFaceSizeLimit(enableFaceSizeLimit)
                .faceSizeLimit(ConfigUtil.getFaceSizeLimit(context))
                .faceMoveLimit(ConfigUtil.getFaceMoveLimit(context))
                .enableLiveness(enableLive)
                .enableImageQuality(enableFaceQualityDetect)
                .maxDetectFaces(ConfigUtil.getRecognizeMaxDetectFaceNum(context))
                .keepMaxFace(ConfigUtil.isKeepMaxFace(context))
                .similarThreshold(ConfigUtil.getRecognizeThreshold(context))
                .imageQualityNoMaskRecognizeThreshold(ConfigUtil.getImageQualityNoMaskRecognizeThreshold(context))
                .imageQualityMaskRecognizeThreshold(ConfigUtil.getImageQualityMaskRecognizeThreshold(context))
                .livenessParam(new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context),
                        ConfigUtil.getLivenessFqThreshold(context)))
                .build();
        int cameraOffsetX = ConfigUtil.getDualCameraHorizontalOffset(context);
        int cameraOffsetY = ConfigUtil.getDualCameraVerticalOffset(context);
        needUpdateFaceData = (livenessType == LivenessType.IR && (cameraOffsetX != 0 || cameraOffsetY != 0));

        ftEngine = new FaceSDK(context);
        int ftEngineMask = FaceSDK.TTV_FACE_DETECT | FaceSDK.TTV_MASK_DETECT;
        ftInitCode.postValue(ftEngine.init(context, DetectMode.TTV_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), ftEngineMask));
        FaceAttributeParam attributeParam = new FaceAttributeParam(
                ConfigUtil.getRecognizeEyeOpenThreshold(context), ConfigUtil.getRecognizeMouthCloseThreshold(context),
                ConfigUtil.getRecognizeWearGlassesThreshold(context));
        ftEngine.setFaceAttributeParam(attributeParam);

        frEngine = new FaceSDK(context);
        int frEngineMask = FaceSDK.TTV_FACE_RECOGNITION;
        if (enableFaceQualityDetect) {
            frEngineMask |= FaceSDK.TTV_IMAGEQUALITY;
        }
        frInitCode.postValue(frEngine.init(context, DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_0_ONLY,
                10, frEngineMask));
        FaceManager.getInstance().initFaceList(context, frEngine, faceCount -> loadFaceList = true, true);

        if (enableLive) {
            flEngine = new FaceSDK(context);
            int flEngineMask = (livenessType == LivenessType.RGB ? FaceSDK.TTV_LIVENESS : (FaceSDK.TTV_IR_LIVENESS | FaceSDK.TTV_FACE_DETECT));
            if (needUpdateFaceData) {
                flEngineMask |= FaceSDK.TTV_UPDATE_FACEDATA;
            }
            flInitCode.postValue(flEngine.init(context, DetectMode.TTV_DETECT_MODE_IMAGE,
                    DetectFaceOrientPriority.TTV_OP_ALL_OUT, 10, flEngineMask));
            LivenessParam livenessParam = new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context), ConfigUtil.getLivenessFqThreshold(context));
            flEngine.setLivenessParam(livenessParam);
        }

        recognizeConfiguration.setValue(configuration);
    }

    private void unInit() {
        if (ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    public void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        List<CompareResult> compareResults = compareResultList.getValue();
        if (compareResults != null) {
            for (int i = compareResults.size() - 1; i >= 0; i--) {
                boolean contains = false;
                for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                    if (facePreviewInfo.getTrackId() == compareResults.get(i).getTrackId()) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    compareResults.remove(i);
                    getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(i, EventType.REMOVED));
                }
            }
        }
    }

    public void destroy() {
        unInit();
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(TTVFaceApplication.getApplication().getApplicationContext(), faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }
        FaceManager.getInstance().release();
        if (registerNv21Disposable != null) {
            registerNv21Disposable.dispose();
            registerNv21Disposable = null;
        }
    }

    public void onRgbCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();

        initFaceHelper(lastPreviewSize);
    }

    public void onIrCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();

        initFaceHelper(lastPreviewSize);
    }

    private void initFaceHelper(Camera.Size lastPreviewSize) {
        if (faceHelper == null || lastPreviewSize == null ||
                lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
            Integer trackedFaceCount = null;

            if (faceHelper != null) {
                trackedFaceCount = faceHelper.getTrackedFaceCount();
                faceHelper.release();
            }
            Context context = TTVFaceApplication.getApplication().getApplicationContext();
            int horizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
            int verticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
            int maxDetectFaceNum = ConfigUtil.getRecognizeMaxDetectFaceNum(context);
            faceHelper = new FaceHelper.Builder()
                    .ftEngine(ftEngine)
                    .frEngine(frEngine)
                    .flEngine(flEngine)
                    .needUpdateFaceData(needUpdateFaceData)
                    .frQueueSize(maxDetectFaceNum)
                    .flQueueSize(maxDetectFaceNum)
                    .previewSize(previewSize)
                    .recognizeCallback(this)
                    .recognizeConfiguration(recognizeConfiguration.getValue())
                    .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(context) : trackedFaceCount)
                    .dualCameraFaceInfoTransformer(faceInfo -> {
                        FaceInfo irFaceInfo = new FaceInfo(faceInfo);
                        irFaceInfo.getRect().offset(horizontalOffset, verticalOffset);
                        return irFaceInfo;
                    })
                    .build();
        }
    }

    @Override
    public void onRecognized(CompareResult compareResult, Integer live, boolean similarPass) {
        Disposable disposable = Observable.just(true).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
            if (similarPass) {
                boolean isAdded = false;
                List<CompareResult> compareResults = compareResultList.getValue();
                if (compareResults != null && !compareResults.isEmpty()) {
                    for (CompareResult compareResult1 : compareResults) {
                        if (compareResult1.getTrackId() == compareResult.getTrackId()) {
                            isAdded = true;
                            break;
                        }
                    }
                }
                if (!isAdded) {
                    if (compareResults != null && compareResults.size() >= MAX_DETECT_NUM) {
                        compareResults.remove(0);
                        getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(0, EventType.REMOVED));
                    }
                    if (compareResults != null) {
                        compareResults.add(compareResult);
                        getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(compareResults.size() - 1, EventType.INSERTED));
                    }
                }
            }
        });
    }

    @Override
    public void onNoticeChanged(String notice) {
        if (recognizeNotice != null) {
            recognizeNotice.postValue(notice);
        }
    }

    public void setDrawRectInfoTextValue(boolean openDrawRect) {
        String stringDrawText = openDrawRect ? "Hide Attr" : "Show Attr";
        if (drawRectInfoText != null) {
            drawRectInfoText.postValue(stringDrawText);
        }
    }

    public void setOnRegisterFinishedCallback(OnRegisterFinishedCallback onRegisterFinishedCallback) {
        this.onRegisterFinishedCallback = onRegisterFinishedCallback;
    }

    public MutableLiveData<Integer> getFtInitCode() {
        return ftInitCode;
    }

    public MutableLiveData<Integer> getFrInitCode() {
        return frInitCode;
    }

    public MutableLiveData<Integer> getFlInitCode() {
        return flInitCode;
    }

    public MutableLiveData<String> getRecognizeNotice() {
        return recognizeNotice;
    }

    public MutableLiveData<String> getDrawRectInfoText() {
        return drawRectInfoText;
    }

    public MutableLiveData<FaceItemEvent> getFaceItemEventMutableLiveData() {
        return faceItemEventMutableLiveData;
    }

    public void prepareRegister() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            updateRegisterStatus(REGISTER_STATUS_READY);
        }
    }

    private void updateRegisterStatus(int status) {
        registerStatus = status;
    }

    public List<FaceRectView.DrawInfo> getDrawInfo(List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType, boolean drawRectInfo) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int trackId = facePreviewInfoList.get(i).getTrackId();
            int mask = facePreviewInfoList.get(i).getMask();

            String name = faceHelper.getName(trackId);
            Integer liveness = faceHelper.getLiveness(trackId);
            Integer recognizeStatus = faceHelper.getRecognizeStatus(trackId);

            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new FaceRectView.DrawInfo(
                    livenessType == LivenessType.RGB ? facePreviewInfoList.get(i).getRgbTransformedRect() : facePreviewInfoList.get(i).getIrTransformedRect(),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? "" : name, facePreviewInfoList.get(i).getFaceInfoRgb().getIsWithinBoundary(),
                    facePreviewInfoList.get(i).getForeRect(), facePreviewInfoList.get(i).getFaceInfoRgb().getFaceAttributeInfo(), drawRectInfo,
                    livenessType == LivenessType.RGB, mask));
        }
        return drawInfoList;
    }

    public List<FacePreviewInfo> onPreviewFrame(byte[] nv21, boolean doRecognize) {
        if (faceHelper != null) {
            if (!loadFaceList) {
                return null;
            }
            if (livenessType == LivenessType.IR && irNV21 == null) {
                return null;
            }
            List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, irNV21, doRecognize);
            if (registerStatus == REGISTER_STATUS_READY && !facePreviewInfoList.isEmpty()) {
                FacePreviewInfo facePreviewInfo = facePreviewInfoList.get(0);
                if (facePreviewInfo.getMask() != MaskInfo.WORN) {
                    registerFace(nv21, facePreviewInfoList.get(0));
                } else {
                    Toast.makeText(TTVFaceApplication.getApplication(), "Please take off your mask to register your face.", Toast.LENGTH_SHORT).show();
                    updateRegisterStatus(REGISTER_STATUS_DONE);
                }
            }
            return facePreviewInfoList;
        }
        return null;
    }

    public void setRecognizeArea(Rect recognizeArea) {
        if (faceHelper != null) {
            faceHelper.setRecognizeArea(recognizeArea);
        }
    }

    public MutableLiveData<RecognizeConfiguration> getRecognizeConfiguration() {
        return recognizeConfiguration;
    }

    public PreviewConfig getPreviewConfig() {
        return previewConfig;
    }

    public Point loadPreviewSize() {
        String[] size = ConfigUtil.getPreviewSize(TTVFaceApplication.getApplication()).split("x");
        return new Point(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
    }
}

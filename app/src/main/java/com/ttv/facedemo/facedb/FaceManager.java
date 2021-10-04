package com.ttv.facedemo.facedb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.ttv.facedemo.TTVFaceApplication;
import com.ttv.facedemo.facedb.entity.FaceEntity;
import com.ttv.facedemo.ui.model.CompareResult;
import com.ttv.facedemo.util.ErrorCodeUtil;
import com.ttv.facedemo.util.ImageUtil;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceFeature;
import com.ttv.face.FaceFeatureInfo;
import com.ttv.face.FaceInfo;
import com.ttv.face.MaskInfo;
import com.ttv.face.SearchResult;
import com.ttv.face.enums.DetectFaceOrientPriority;
import com.ttv.face.enums.DetectMode;
import com.ttv.face.enums.ExtractType;
import com.ttv.imageutil.TTVImageFormat;
import com.ttv.imageutil.TTVImageUtil;
import com.ttv.imageutil.TTVImageUtilError;
import com.ttv.imageutil.TTVRotateDegree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class FaceManager {
    private static final String TAG = "FaceManager";
    private static FaceSDK faceEngine = null;
    private static volatile FaceManager faceServer = null;
    private List<FaceEntity> faceRegisterInfoList;
    private String imageRootPath;

    private static final int MAX_REGISTER_FACE_COUNT = 30000;

    private FaceManager() {
        faceRegisterInfoList = new ArrayList<>();
    }

    public static FaceManager getInstance() {
        if (faceServer == null) {
            synchronized (FaceManager.class) {
                if (faceServer == null) {
                    faceServer = new FaceManager();
                }
            }
        }
        return faceServer;
    }

    public interface OnInitFinishedCallback {
        void onFinished(int faceCount);
    }

    public void init(Context context) {
        init(context, null);
    }

    public synchronized void init(Context context, OnInitFinishedCallback onInitFinishedCallback) {
        if (faceEngine == null && context != null) {
            faceEngine = new FaceSDK(context);
            int engineCode = faceEngine.init(context, DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_ALL_OUT,
                    1, FaceSDK.TTV_FACE_RECOGNITION | FaceSDK.TTV_FACE_DETECT | FaceSDK.TTV_MASK_DETECT);
            if (engineCode == ErrorInfo.MOK) {
                initFaceList(context, null, onInitFinishedCallback, false);
            } else {
                faceEngine = null;
                Log.e(TAG, "init: failed! code = " + engineCode);
            }
        }
        if (faceRegisterInfoList != null && onInitFinishedCallback != null) {
            onInitFinishedCallback.onFinished(faceRegisterInfoList.size());
        }
    }

    public synchronized void release() {
        if (faceRegisterInfoList != null) {
            faceRegisterInfoList.clear();
            faceRegisterInfoList = null;
        }
        if (faceEngine != null) {
            synchronized (faceEngine) {
                faceEngine.unInit();
            }
            faceEngine = null;
        }
        faceServer = null;
    }

    public void initFaceList(final Context context, FaceSDK faceEngine, final OnInitFinishedCallback onInitFinishedCallback, boolean recognize) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            if (recognize) {
                List<FaceEntity> faceEntityList = FaceDatabase.getInstance(context).faceDao().getAllFaces();
                registerFaceFeatureInfoListFromDb(faceEngine, faceEntityList);
                emitter.onNext(faceEntityList.size());
            } else {
                faceRegisterInfoList = FaceDatabase.getInstance(context).faceDao().getAllFaces();
                emitter.onNext(faceRegisterInfoList == null ? 0 : faceRegisterInfoList.size());
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(size -> {
                    if (onInitFinishedCallback != null) {
                        onInitFinishedCallback.onFinished(size);
                    }
                });
    }

    public synchronized void removeOneFace(FaceEntity faceEntity) {
        if (faceRegisterInfoList != null) {
            faceRegisterInfoList.remove(faceEntity);
        }
    }

    @SuppressLint("CheckResult")
    public synchronized int clearAllFaces() {
        if (faceRegisterInfoList != null) {
            faceRegisterInfoList.clear();
        }
        if (faceEngine != null) {
            faceEngine.removeFaceFeature(-1);
        }
        Context applicationContext = TTVFaceApplication.getApplication().getApplicationContext();
        int deleteSize = FaceDatabase.getInstance(applicationContext).faceDao().deleteAll();
        File imgDir = new File(getImageDir());
        File[] files = imgDir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                file.delete();
            }
        }
        return deleteSize;
    }

    public boolean registerNv21(Context context, byte[] nv21, int width, int height, FacePreviewInfo faceInfo, String name,
                                FaceSDK frEngine, FaceSDK registerFaceEngine) {
        if (registerFaceEngine == null || context == null || nv21 == null || width % 4 != 0 || nv21.length != width * height * 3 / 2) {
            Log.e(TAG, "registerNv21: invalid params");
            return false;
        }
        FaceFeature faceFeature = new FaceFeature();
        int code;

        synchronized (registerFaceEngine) {
            code = registerFaceEngine.extractFaceFeature(nv21, width, height, FaceSDK.CP_PAF_NV21, faceInfo.getFaceInfoRgb(),
                    ExtractType.REGISTER, MaskInfo.NOT_WORN, faceFeature);
        }
        if (code != ErrorInfo.MOK) {
            Log.e(TAG, "registerNv21: extractFaceFeature failed , code is " + code);
            return false;
        } else {

            Rect cropRect = getBestRect(width, height, faceInfo.getFaceInfoRgb().getRect());
            if (cropRect == null) {
                Log.e(TAG, "registerNv21: cropRect is null!");
                return false;
            }

            cropRect.left &= ~3;
            cropRect.top &= ~3;
            cropRect.right &= ~3;
            cropRect.bottom &= ~3;

            Bitmap headBmp = getHeadImage(nv21, width, height, faceInfo.getFaceInfoRgb().getOrient(), cropRect, TTVImageFormat.NV21);
            String imgPath = getImagePath(name);
            try {
                FileOutputStream fos = new FileOutputStream(imgPath);
                headBmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            FaceEntity faceEntity = new FaceEntity(name, imgPath, faceFeature.getFeatureData());
            long faceId = FaceDatabase.getInstance(context).faceDao().insert(faceEntity);
            faceEntity.setFaceId(faceId);
            registerFaceFeatureInfoFromDb(faceEntity, frEngine);
            return true;
        }
    }

    private void registerFaceFeatureInfoListFromDb(FaceSDK faceEngine, List<FaceEntity> faceEntityList) {
        List<FaceFeatureInfo> faceFeatureInfoList = new ArrayList<>();
        for (FaceEntity faceEntity : faceEntityList) {
            FaceFeatureInfo faceFeatureInfo = new FaceFeatureInfo((int) faceEntity.getFaceId(), faceEntity.getFeatureData());
            faceFeatureInfoList.add(faceFeatureInfo);
        }
        if (faceEngine != null) {
            faceEngine.removeFaceFeature(-1);
            int res = faceEngine.registerFaceFeature(faceFeatureInfoList);
            Log.i(TAG, "registerFaceFeature:" + res);
        }
    }

    private void registerFaceFeatureInfoFromDb(FaceEntity faceEntity, FaceSDK faceEngine) {
        if (faceEntity != null && faceEngine != null) {
            FaceFeatureInfo faceFeatureInfo = new FaceFeatureInfo((int) faceEntity.getFaceId(), faceEntity.getFeatureData());
            int res = faceEngine.registerFaceFeature(faceFeatureInfo);
            Log.i(TAG, "registerFaceFeature:" + res);
        }
    }

    private String getImageDir() {
        return TTVFaceApplication.getApplication().getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + "faceDB" + File.separator + "registerFaces";
    }

    private String getImagePath(String name) {
        if (imageRootPath == null) {
            imageRootPath = getImageDir();
            File dir = new File(imageRootPath);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
        }
        return imageRootPath + File.separator + name + "_" + System.currentTimeMillis() + ".jpg";
    }

    public FaceEntity registerJpeg(Context context, byte[] jpeg, String name) throws RegisterFailedException {
        if (faceRegisterInfoList != null && faceRegisterInfoList.size() >= MAX_REGISTER_FACE_COUNT) {
            Log.e(TAG, "registerJpeg: registered face count limited " + faceRegisterInfoList.size());
            throw new RegisterFailedException("registered face count limited");
        }
        Bitmap bitmap = ImageUtil.jpegToScaledBitmap(jpeg, ImageUtil.DEFAULT_MAX_WIDTH, ImageUtil.DEFAULT_MAX_HEIGHT);
        bitmap = TTVImageUtil.getAlignedBitmap(bitmap, true);
        byte[] imageData = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
        int code = TTVImageUtil.bitmapToImageData(bitmap, imageData, TTVImageFormat.BGR24);
        if (code != TTVImageUtilError.CODE_SUCCESS) {
            throw new RuntimeException("bitmapToImageData failed, code is " + code);
        }
        return registerBgr24(context, imageData, bitmap.getWidth(), bitmap.getHeight(), name);
    }

    public FaceEntity registerBgr24(Context context, byte[] bgr24, int width, int height, String name) {
        if (faceEngine == null || context == null || bgr24 == null || width % 4 != 0 || bgr24.length != width * height * 3) {
            Log.e(TAG, "registerBgr24:  invalid params");
            return null;
        }
        //人脸检测
        List<FaceInfo> faceInfoList = new ArrayList<>();
        int code;
        synchronized (faceEngine) {
            code = faceEngine.detectFaces(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList);
        }
        if (code == ErrorInfo.MOK && !faceInfoList.isEmpty()) {
            code = faceEngine.process(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList,
                    FaceSDK.TTV_MASK_DETECT);
            if (code == ErrorInfo.MOK) {
                List<MaskInfo> maskInfoList = new ArrayList<>();
                faceEngine.getMask(maskInfoList);
                if (!maskInfoList.isEmpty()) {
                    int isMask = maskInfoList.get(0).getMask();
                    if (isMask == MaskInfo.WORN) {
                         Log.e(TAG, "registerBgr24: maskInfo is worn");
                        return null;
                    }
                }
            }

            FaceFeature faceFeature = new FaceFeature();

            synchronized (faceEngine) {
                code = faceEngine.extractFaceFeature(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList.get(0),
                        ExtractType.REGISTER, MaskInfo.NOT_WORN, faceFeature);
            }
            String userName = name == null ? String.valueOf(System.currentTimeMillis()) : name;

            if (code == ErrorInfo.MOK) {
                Rect cropRect = getBestRect(width, height, faceInfoList.get(0).getRect());
                if (cropRect == null) {
                    Log.e(TAG, "registerBgr24: cropRect is null");
                    return null;
                }

                cropRect.left &= ~3;
                cropRect.top &= ~3;
                cropRect.right &= ~3;
                cropRect.bottom &= ~3;

                String imgPath = getImagePath(userName);

                Bitmap headBmp = getHeadImage(bgr24, width, height, faceInfoList.get(0).getOrient(), cropRect, TTVImageFormat.BGR24);

                try {
                    FileOutputStream fos = new FileOutputStream(imgPath);
                    headBmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                if (faceRegisterInfoList == null) {
                    faceRegisterInfoList = new ArrayList<>();
                }
                FaceEntity faceEntity = new FaceEntity(name, imgPath, faceFeature.getFeatureData());
                long faceId = FaceDatabase.getInstance(context).faceDao().insert(faceEntity);
                faceEntity.setFaceId(faceId);
                faceRegisterInfoList.add(faceEntity);
                return faceEntity;
            } else {
                Log.e(TAG, "registerBgr24: extract face feature failed, code is " + code);
                return null;
            }
        } else {
            Log.e(TAG, "registerBgr24: no face detected, code is " + code);
            return null;
        }
    }

    private Bitmap getHeadImage(byte[] originImageData, int width, int height, int orient, Rect cropRect, TTVImageFormat imageFormat) {
        byte[] headImageData = TTVImageUtil.createImageData(cropRect.width(), cropRect.height(), imageFormat);
        int cropCode = TTVImageUtil.cropImage(originImageData, headImageData, width, height, cropRect, imageFormat);
        if (cropCode != TTVImageUtilError.CODE_SUCCESS) {
            throw new RuntimeException("crop image failed, code is " + cropCode);
        }

        byte[] rotateHeadImageData = null;
        int cropImageWidth;
        int cropImageHeight;
        if (orient == FaceSDK.TTV_OC_90 || orient == FaceSDK.TTV_OC_270) {
            cropImageWidth = cropRect.height();
            cropImageHeight = cropRect.width();
        } else {
            cropImageWidth = cropRect.width();
            cropImageHeight = cropRect.height();
        }
        TTVRotateDegree rotateDegree = null;
        switch (orient) {
            case FaceSDK.TTV_OC_90:
                rotateDegree = TTVRotateDegree.DEGREE_270;
                break;
            case FaceSDK.TTV_OC_180:
                rotateDegree = TTVRotateDegree.DEGREE_180;
                break;
            case FaceSDK.TTV_OC_270:
                rotateDegree = TTVRotateDegree.DEGREE_90;
                break;
            case FaceSDK.TTV_OC_0:
            default:
                rotateHeadImageData = headImageData;
                break;
        }
        if (rotateDegree != null) {
            rotateHeadImageData = new byte[headImageData.length];
            int rotateCode = TTVImageUtil.rotateImage(headImageData, rotateHeadImageData, cropRect.width(), cropRect.height(), rotateDegree, imageFormat);
            if (rotateCode != TTVImageUtilError.CODE_SUCCESS) {
                throw new RuntimeException("rotate image failed, code is : " + rotateCode + ", code description is : " + ErrorCodeUtil.imageUtilErrorCodeToFieldName(rotateCode));
            }
        }
        Bitmap headBmp = Bitmap.createBitmap(cropImageWidth, cropImageHeight, Bitmap.Config.RGB_565);
        int imageDataToBitmapCode = TTVImageUtil.imageDataToBitmap(rotateHeadImageData, headBmp, imageFormat);
        if (imageDataToBitmapCode != TTVImageUtilError.CODE_SUCCESS) {
            throw new RuntimeException("failed to transform image data to bitmap, code is : " + imageDataToBitmapCode
                    + ", code description is : " + ErrorCodeUtil.imageUtilErrorCodeToFieldName(imageDataToBitmapCode));
        }
        return headBmp;
    }

    public CompareResult searchFaceFeature(FaceFeature faceFeature, FaceSDK faceEngine) {
        if (faceEngine == null || faceFeature == null) {
            return null;
        }
        long start = System.currentTimeMillis();
        SearchResult searchResult;
        try {
            long searchStart = System.currentTimeMillis();
            searchResult = faceEngine.searchFaceFeature(faceFeature);
            Log.i(TAG, "searchCost:" + (System.currentTimeMillis() - searchStart) + "ms");
            if (searchResult != null) {
                FaceFeatureInfo faceFeatureInfo = searchResult.getFaceFeatureInfo();
                FaceEntity faceEntity = FaceDatabase.getInstance(TTVFaceApplication.getApplication()).faceDao().queryByFaceId(faceFeatureInfo.getSearchId());
                if (faceEntity != null) {
                    return new CompareResult(faceEntity, searchResult.getMaxSimilar(), ErrorInfo.MOK, System.currentTimeMillis() - start);
                }
            }
        } catch (IllegalArgumentException exception) {
            Log.i(TAG, "exception:" + exception.getMessage());
        }
        return null;
    }

    private static Rect getBestRect(int width, int height, Rect srcRect) {
        if (srcRect == null) {
            return null;
        }
        Rect rect = new Rect(srcRect);

        int maxOverFlow = Math.max(-rect.left, Math.max(-rect.top, Math.max(rect.right - width, rect.bottom - height)));
        if (maxOverFlow >= 0) {
            rect.inset(maxOverFlow, maxOverFlow);
            return rect;
        }

        int padding = rect.height() / 2;

        if (!(rect.left - padding > 0 && rect.right + padding < width && rect.top - padding > 0 && rect.bottom + padding < height)) {
            padding = Math.min(Math.min(Math.min(rect.left, width - rect.right), height - rect.bottom), rect.top);
        }
        rect.inset(-padding, -padding);
        return rect;
    }
}

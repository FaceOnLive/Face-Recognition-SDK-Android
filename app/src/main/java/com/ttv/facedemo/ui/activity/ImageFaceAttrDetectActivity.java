package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.ParcelableSpan;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.ttv.facedemo.R;
import com.ttv.facedemo.util.ErrorCodeUtil;
import com.ttv.facedemo.util.ImageUtil;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceInfo;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.MaskInfo;
import com.ttv.face.enums.DetectFaceOrientPriority;
import com.ttv.face.enums.DetectMode;
import com.ttv.face.enums.DetectModel;
import com.ttv.imageutil.TTVImageFormat;
import com.ttv.imageutil.TTVImageUtil;
import com.ttv.imageutil.TTVImageUtilError;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImageFaceAttrDetectActivity extends BaseActivity {
    private static final String TAG = "ImageFaceAttrDetect";
    private ImageView ivShow;
    private TextView tvNotice;
    private FaceSDK faceEngine;
    private int faceEngineCode = -1;

    int processMask = FaceSDK.TTV_AGE | FaceSDK.TTV_GENDER | FaceSDK.TTV_LIVENESS | FaceSDK.TTV_MASK_DETECT;

    private AlertDialog progressDialog;
    private Bitmap mBitmap = null;
    private static String[] NEEDED_PERMISSIONS = new String[]{
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_face_attr_detect);
        initView();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            List<String> permissionList = new ArrayList<>(Arrays.asList(NEEDED_PERMISSIONS));
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            NEEDED_PERMISSIONS = permissionList.toArray(new String[0]);
        }

        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
        }

    }

    private void initEngine() {
        faceEngine = new FaceSDK(this);
        faceEngineCode = faceEngine.init(this, DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_ALL_OUT,
                5, FaceSDK.TTV_FACE_RECOGNITION | FaceSDK.TTV_FACE_DETECT | processMask);
        Log.i(TAG, "initEngine: init: " + faceEngineCode);

        if (faceEngineCode != ErrorInfo.MOK) {
            showToast(getString(R.string.init_failed, faceEngineCode, ErrorCodeUtil.ttvErrorCodeToFieldName(faceEngineCode)));
        }
    }

    private void unInitEngine() {
        if (faceEngine != null) {
            faceEngineCode = faceEngine.unInit();
            faceEngine = null;
            Log.i(TAG, "unInitEngine: " + faceEngineCode);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;

        unInitEngine();
        super.onDestroy();
    }

    private void initView() {
        tvNotice = findViewById(R.id.tv_notice);
        ivShow = findViewById(R.id.iv_show);
        progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.processing)
                .setView(new ProgressBar(this))
                .create();
    }

    public void process(final View view) {

        view.setClickable(false);
        if (progressDialog == null || progressDialog.isShowing()) {
            return;
        }
        progressDialog.show();
        Observable.create(emitter -> {
            processImage();
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        view.setClickable(true);
                    }
                });
    }

    public void processImage() {
        if (mBitmap == null) {
            return;
        }
        Bitmap bitmap = TTVImageUtil.getAlignedBitmap(mBitmap, true);

        final SpannableStringBuilder notificationSpannableStringBuilder = new SpannableStringBuilder();
        if (faceEngineCode != ErrorInfo.MOK) {
            addNotificationInfo(notificationSpannableStringBuilder, null, " face engine not initialized!");
            showNotificationAndFinish(notificationSpannableStringBuilder);
            return;
        }
        if (bitmap == null) {
            addNotificationInfo(notificationSpannableStringBuilder, null, " bitmap is null!");
            showNotificationAndFinish(notificationSpannableStringBuilder);
            return;
        }
        if (faceEngine == null) {
            addNotificationInfo(notificationSpannableStringBuilder, null, " faceEngine is null!");
            showNotificationAndFinish(notificationSpannableStringBuilder);
            return;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        long start = System.currentTimeMillis();
        byte[] bgr24 = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
        int transformCode = TTVImageUtil.bitmapToImageData(bitmap, bgr24, TTVImageFormat.BGR24);
        if (transformCode != TTVImageUtilError.CODE_SUCCESS) {
            Log.e(TAG, "transform failed, code is " + transformCode);
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "transform bitmap To ImageData failed", "code is ", String.valueOf(transformCode), "\n");
            return;
        }
//        Log.i(TAG, "processImage:bitmapToBgr24 cost =  " + (System.currentTimeMillis() - start));
        addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "start face detection,imageWidth is ", String.valueOf(width), ", imageHeight is ", String.valueOf(height), "\n");

        List<FaceInfo> faceInfoList = new ArrayList<>();

        long fdStartTime = System.currentTimeMillis();
//        TTVImageInfo ttvImageInfo = new TTVImageInfo(width,height,FaceEngine.CP_PAF_BGR24,new byte[][]{bgr24},new int[]{width * 3});
//        Log.i(TAG, "processImage: " + ttvImageInfo.getPlanes()[0].length);
//        int detectCode = faceEngine.detectFaces(ttvImageInfo, faceInfoList);
        int detectCode = faceEngine.detectFaces(bgr24, width, height, FaceSDK.CP_PAF_BGR24, DetectModel.RGB, faceInfoList);
        if (detectCode == ErrorInfo.MOK) {
//            Log.i(TAG, "processImage: fd costTime = " + (System.currentTimeMillis() - fdStartTime));
        }

        Bitmap bitmapForDraw = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmapForDraw);
        Paint paint = new Paint();
        addNotificationInfo(notificationSpannableStringBuilder, null, "detect result:\nerrorCode is :", String.valueOf(detectCode), "   face Number is ", String.valueOf(faceInfoList.size()), "\n");

        if (faceInfoList.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face list:\n");
            paint.setAntiAlias(true);
            paint.setStrokeWidth(5);
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < faceInfoList.size(); i++) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(faceInfoList.get(i).getRect(), paint);

                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                int textSize = faceInfoList.get(i).getRect().width() / 2;
                paint.setTextSize(textSize);

                canvas.drawText(String.valueOf(i), faceInfoList.get(i).getRect().left, faceInfoList.get(i).getRect().top - 10, paint);
                addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", faceInfoList.get(i).toString(), "\n");
            }
            final Bitmap finalBitmapForDraw = bitmapForDraw;
            runOnUiThread(() -> Glide.with(ivShow.getContext())
                    .load(finalBitmapForDraw)
                    .into(ivShow));
        } else {
            addNotificationInfo(notificationSpannableStringBuilder, null, "can not do further action, exit!");
            showNotificationAndFinish(notificationSpannableStringBuilder);
            return;
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");


        long processStartTime = System.currentTimeMillis();
        int faceProcessCode = faceEngine.process(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList, processMask);

        if (faceProcessCode != ErrorInfo.MOK) {
            addNotificationInfo(notificationSpannableStringBuilder, new ForegroundColorSpan(Color.RED), "process failed! code is ", String.valueOf(faceProcessCode), "\n");
        } else {
            Log.i(TAG, "processImage: process costTime = " + (System.currentTimeMillis() - processStartTime));
        }
        List<AgeInfo> ageInfoList = new ArrayList<>();
        List<GenderInfo> genderInfoList = new ArrayList<>();
        List<LivenessInfo> livenessInfoList = new ArrayList<>();
        List<MaskInfo> maskInfoList = new ArrayList<>();
        int ageCode = faceEngine.getAge(ageInfoList);
        int genderCode = faceEngine.getGender(genderInfoList);
        int livenessCode = faceEngine.getLiveness(livenessInfoList);
        int maskCode = faceEngine.getMask(maskInfoList);

        if ((ageCode | genderCode | livenessCode | maskCode) != ErrorInfo.MOK) {
            addNotificationInfo(notificationSpannableStringBuilder, null,
                    "at least one of age, gender, liveness, mask detect failed, codes are:",
                    String.valueOf(ageCode), " , ", String.valueOf(genderCode), " , ", String.valueOf(livenessCode), " , ", String.valueOf(maskCode));
            showNotificationAndFinish(notificationSpannableStringBuilder);
            return;
        }

        if (ageInfoList.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "age of each face:\n");
        }
        for (int i = 0; i < ageInfoList.size(); i++) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", String.valueOf(ageInfoList.get(i).getAge()), "\n");
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");

        if (genderInfoList.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "gender of each face:\n");
        }
        for (int i = 0; i < genderInfoList.size(); i++) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:"
                    , genderInfoList.get(i).getGender() == GenderInfo.MALE ?
                            "MALE" : (genderInfoList.get(i).getGender() == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN"), "\n");
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");

        if (livenessInfoList.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "liveness of each face:\n");
            for (int i = 0; i < livenessInfoList.size(); i++) {
                String liveness = null;
                switch (livenessInfoList.get(i).getLiveness()) {
                    case LivenessInfo.ALIVE:
                        liveness = "REAL";
                        break;
                    case LivenessInfo.NOT_ALIVE:
                        liveness = "FAKE";
                        break;
                    case LivenessInfo.FACE_NUM_MORE_THAN_ONE:
                        liveness = "FACE_NUM_MORE_THAN_ONE";
                        break;
                    case LivenessInfo.UNKNOWN:
                    default:
                        liveness = "";
                        break;
                }
                addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", liveness, "\n");
            }
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");

        if (!maskInfoList.isEmpty()) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "mask of each face:\n");
            for (int i = 0; i < maskInfoList.size(); i++) {
                MaskInfo maskInfo = maskInfoList.get(i);
                int mask = maskInfo.getMask();
                String stringMask;
                switch (mask) {
                    case MaskInfo.NOT_WORN:
                        stringMask = "No Mask";
                        break;
                    case MaskInfo.WORN:
                        stringMask = "Mask";
                        break;
                    default:
                        stringMask = "Uncertain Mask";
                        break;
                }
                addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", stringMask, "\n");
            }
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");
        showNotificationAndFinish(notificationSpannableStringBuilder);
    }

    private void showNotificationAndFinish(final SpannableStringBuilder stringBuilder) {
        runOnUiThread(() -> {
            if (tvNotice != null) {
                tvNotice.setText(stringBuilder);
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void addNotificationInfo(SpannableStringBuilder stringBuilder, ParcelableSpan styleSpan, String... strings) {
        if (stringBuilder == null || strings == null || strings.length == 0) {
            return;
        }
        int startLength = stringBuilder.length();
        for (String string : strings) {
            stringBuilder.append(string);
        }
        int endLength = stringBuilder.length();
        if (styleSpan != null) {
            stringBuilder.setSpan(styleSpan, startLength, endLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void chooseLocalImage(View view) {
        getImageFromAlbum(new OnGetImageFromAlbumCallback() {
            @Override
            public void onGetImageFromAlbumSuccess(Uri uri) {
                mBitmap = ImageUtil.uriToScaledBitmap(ImageFaceAttrDetectActivity.this, uri, ImageUtil.DEFAULT_MAX_WIDTH, ImageUtil.DEFAULT_MAX_HEIGHT);
                Glide.with(ivShow.getContext())
                        .load(mBitmap)
                        .into(ivShow);
                tvNotice.setText("");

                processImage();
            }

            @Override
            public void onGetImageFromAlbumFailed() {
                showToast(getString(R.string.get_picture_failed));
            }
        });
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initEngine();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }
}

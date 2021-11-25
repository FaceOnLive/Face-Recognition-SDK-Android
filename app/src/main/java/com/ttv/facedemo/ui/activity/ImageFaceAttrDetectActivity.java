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

import com.ttv.face.FaceEngine;
import com.ttv.face.FaceResult;
import com.ttv.facedemo.R;
import com.ttv.facedemo.util.ErrorCodeUtil;
import com.ttv.facedemo.util.ImageUtil;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
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
    }

    private void unInitEngine() {

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
        if (bitmap == null) {
            addNotificationInfo(notificationSpannableStringBuilder, null, " bitmap is null!");
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
        addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "start face detection,imageWidth is ", String.valueOf(width), ", imageHeight is ", String.valueOf(height), "\n");

        long fdStartTime = System.currentTimeMillis();
        List<FaceResult> faceResults = FaceEngine.getInstance(this).detectFace(bitmap);

        Bitmap bitmapForDraw = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmapForDraw);
        Paint paint = new Paint();
        addNotificationInfo(notificationSpannableStringBuilder, null, "detect result:\n", "   face Number is ", String.valueOf(faceResults.size()), "\n");

        if (faceResults.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face list:\n");
            paint.setAntiAlias(true);
            paint.setStrokeWidth(5);
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < faceResults.size(); i++) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(faceResults.get(i).rect, paint);

                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                int textSize = faceResults.get(i).rect.width() / 2;
                paint.setTextSize(textSize);

                canvas.drawText(String.valueOf(i), faceResults.get(i).rect.left, faceResults.get(i).rect.top - 10, paint);

                String angleInfo = "Face3DAngle{yaw=" + faceResults.get(i).yaw + ", roll=" + faceResults.get(i).roll + ", pitch=" + faceResults.get(i).pitch + '}';
                String faceInfo = "FaceInfo{faceRect=" + faceResults.get(i).rect.toString() + ", orient=" + faceResults.get(i).orient + ", " + angleInfo + '}';
                addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", faceInfo, "\n");
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
        int faceProcessCode = FaceEngine.getInstance(this).faceAttrProcess(bitmap, faceResults);

        if (faceProcessCode != ErrorInfo.MOK) {
            addNotificationInfo(notificationSpannableStringBuilder, new ForegroundColorSpan(Color.RED), "process failed! code is ", String.valueOf(faceProcessCode), "\n");
        } else {
            Log.i(TAG, "processImage: process costTime = " + (System.currentTimeMillis() - processStartTime));
        }

        if (faceResults.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "age of each face:\n");
        }
        for (int i = 0; i < faceResults.size(); i++) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:", String.valueOf(faceResults.get(i).age), "\n");
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");

        if (faceResults.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "gender of each face:\n");
        }
        for (int i = 0; i < faceResults.size(); i++) {
            addNotificationInfo(notificationSpannableStringBuilder, null, "face[", String.valueOf(i), "]:"
                    , faceResults.get(i).gender == GenderInfo.MALE ?
                            "MALE" : (faceResults.get(i).gender == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN"), "\n");
        }
        addNotificationInfo(notificationSpannableStringBuilder, null, "\n");

        if (faceResults.size() > 0) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "liveness of each face:\n");
            for (int i = 0; i < faceResults.size(); i++) {
                String liveness = null;
                switch (faceResults.get(i).liveness) {
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

        if (!faceResults.isEmpty()) {
            addNotificationInfo(notificationSpannableStringBuilder, new StyleSpan(Typeface.BOLD), "mask of each face:\n");
            for (int i = 0; i < faceResults.size(); i++) {
                int mask = faceResults.get(i).mask;
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

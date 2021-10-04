package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ttv.facedemo.R;
import com.ttv.facedemo.ui.adapter.MultiFaceInfoAdapter;
import com.ttv.facedemo.ui.model.ItemShowInfo;
import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.facedemo.util.ErrorCodeUtil;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceFeature;
import com.ttv.face.FaceInfo;
import com.ttv.face.FaceSimilar;
import com.ttv.face.GenderInfo;
import com.ttv.face.ImageQualitySimilar;
import com.ttv.face.MaskInfo;
import com.ttv.face.enums.DetectFaceOrientPriority;
import com.ttv.face.enums.DetectMode;
import com.ttv.face.enums.ExtractType;
import com.ttv.imageutil.TTVImageFormat;
import com.ttv.imageutil.TTVImageUtil;
import com.ttv.imageutil.TTVImageUtilError;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaceCompareActivity extends BaseActivity {

    private static final String TAG = "MultiImageActivity";

    private static final int ACTION_CHOOSE_MAIN_IMAGE = 0x201;
    private static final int ACTION_ADD_RECYCLER_ITEM_IMAGE = 0x202;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final int INIT_MASK = FaceSDK.TTV_FACE_RECOGNITION | FaceSDK.TTV_FACE_DETECT | FaceSDK.TTV_GENDER |
            FaceSDK.TTV_AGE | FaceSDK.TTV_MASK_DETECT | FaceSDK.TTV_IMAGEQUALITY;

    private ImageView ivMainImage;
    private TextView tvMainImageInfo;


    private static final int TYPE_MAIN = 0;
    private static final int TYPE_ITEM = 1;

    private FaceFeature mainFeature;
    private MultiFaceInfoAdapter multiFaceInfoAdapter;
    private List<ItemShowInfo> showInfoList;

    private FaceSDK mainFaceEngine;
    private int faceEngineCode = -1;
    private Bitmap mainBitmap;

    private String[] neededPermissions = new String[]{
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_multi_image);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            List<String> permissionList = new ArrayList<>(Arrays.asList(neededPermissions));
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            neededPermissions = permissionList.toArray(new String[0]);
        }

        if (!checkPermissions(neededPermissions)) {
            ActivityCompat.requestPermissions(this, neededPermissions, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
        }
        initView();
    }

    private void initView() {
        ivMainImage = findViewById(R.id.iv_main_image);
        tvMainImageInfo = findViewById(R.id.tv_main_image_info);
        RecyclerView recyclerFaces = findViewById(R.id.recycler_faces);
        showInfoList = new ArrayList<>();
        multiFaceInfoAdapter = new MultiFaceInfoAdapter(showInfoList, this);
        recyclerFaces.setAdapter(multiFaceInfoAdapter);
        recyclerFaces.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerFaces.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initEngine() {
        mainFaceEngine = new FaceSDK(this);
        faceEngineCode = mainFaceEngine.init(this, DetectMode.TTV_DETECT_MODE_IMAGE, DetectFaceOrientPriority.TTV_OP_ALL_OUT,
                6, INIT_MASK);
        if (faceEngineCode != ErrorInfo.MOK) {
            showToast(getString(R.string.init_failed, faceEngineCode, ErrorCodeUtil.ttvErrorCodeToFieldName(faceEngineCode)));
        }
    }

    private void unInitEngine() {
        if (mainFaceEngine != null) {
            faceEngineCode = mainFaceEngine.unInit();
            Log.i(TAG, "unInitEngine: " + faceEngineCode);
        }
    }

    @Override
    protected void onDestroy() {
        unInitEngine();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || data.getData() == null) {
            showToast(getString(R.string.get_picture_failed));
            return;
        }
        if (requestCode == ACTION_CHOOSE_MAIN_IMAGE) {
            try {
                mainBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            if (mainBitmap == null) {
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            processImage(mainBitmap, TYPE_MAIN);
        } else if (requestCode == ACTION_ADD_RECYCLER_ITEM_IMAGE) {
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            if (bitmap == null) {
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            if (mainFeature == null) {
                return;
            }
            processImage(bitmap, TYPE_ITEM);
        }
    }

    public void processImage(Bitmap bitmap, int type) {
        if (bitmap == null) {
            return;
        }
        if (mainFaceEngine == null) {
            return;
        }
        bitmap = TTVImageUtil.getAlignedBitmap(bitmap, true);
        if (bitmap == null) {
            return;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] bgr24 = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
        int transformCode = TTVImageUtil.bitmapToImageData(bitmap, bgr24, TTVImageFormat.BGR24);
        if (transformCode != TTVImageUtilError.CODE_SUCCESS) {
            showToast("failed to transform bitmap to imageData, code is " + transformCode);
            return;
        }
        List<FaceInfo> faceInfoList = new ArrayList<>();
        int detectCode = mainFaceEngine.detectFaces(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList);
        if (detectCode != 0 || faceInfoList.isEmpty()) {
            showToast("face detection finished, code is " + detectCode + ", face num is " + faceInfoList.size());
            return;
        }
        bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        if (!faceInfoList.isEmpty()) {
            for (int i = 0; i < faceInfoList.size(); i++) {
                 paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(faceInfoList.get(i).getRect(), paint);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setTextSize((float) faceInfoList.get(i).getRect().width() / 2);
                canvas.drawText("" + i, faceInfoList.get(i).getRect().left, faceInfoList.get(i).getRect().top, paint);
            }
        }
        int faceProcessCode = mainFaceEngine.process(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList,
                FaceSDK.TTV_AGE | FaceSDK.TTV_GENDER | FaceSDK.TTV_MASK_DETECT);
        if (faceProcessCode != ErrorInfo.MOK) {
            showToast("face process finished, code is " + faceProcessCode);
            return;
        }
        List<AgeInfo> ageInfoList = new ArrayList<>();
        List<GenderInfo> genderInfoList = new ArrayList<>();
        List<MaskInfo> maskInfoList = new ArrayList<>();
        int ageCode = mainFaceEngine.getAge(ageInfoList);
        int genderCode = mainFaceEngine.getGender(genderInfoList);
        int maskInfoCode = mainFaceEngine.getMask(maskInfoList);
        if ((ageCode | genderCode | maskInfoCode) != ErrorInfo.MOK) {
            showToast("at lease one of age、gender、mask detect failed! codes are: " + ageCode
                    + " ," + genderCode + " ," + maskInfoCode);
            return;
        }

        int isMask = MaskInfo.UNKNOWN;
        if (!maskInfoList.isEmpty()) {
            isMask = maskInfoList.get(0).getMask();
        }

        if (isMask == MaskInfo.UNKNOWN) {
            showToast("mask is unknown");
            return;
        }

        if (type == TYPE_MAIN && isMask == MaskInfo.WORN) {
            showToast(getResources().getString(R.string.notice_register_image_no_mask));
            return;
        }

        ImageQualitySimilar imageQualitySimilar = new ImageQualitySimilar();
        int qualityCode = mainFaceEngine.imageQualityDetect(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList.get(0),
                isMask, imageQualitySimilar);
        if (qualityCode != ErrorInfo.MOK) {
            showToast("imageQualityDetect failed! code is " + qualityCode);
            return;
        }
        float quality = imageQualitySimilar.getScore();
        float destQuality;
        if (type == TYPE_MAIN) {

            destQuality = ConfigUtil.getImageQualityNoMaskRegisterThreshold(this);
        } else {

            if (isMask == MaskInfo.WORN) {

                destQuality = ConfigUtil.getImageQualityMaskRecognizeThreshold(this);
            } else {

                destQuality = ConfigUtil.getImageQualityNoMaskRecognizeThreshold(this);
            }
        }
        if (quality < destQuality) {
            showToast("image quality invalid");
            return;
        }

        if (!faceInfoList.isEmpty()) {
            if (type == TYPE_MAIN) {
                int size = showInfoList.size();
                showInfoList.clear();
                multiFaceInfoAdapter.notifyItemRangeRemoved(0, size);
                mainFeature = new FaceFeature();
                int res = mainFaceEngine.extractFaceFeature(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList.get(0),
                        ExtractType.REGISTER, isMask, mainFeature);
                if (res != ErrorInfo.MOK) {
                    mainFeature = null;
                }
                Glide.with(ivMainImage.getContext())
                        .load(bitmap)
                        .into(ivMainImage);
                StringBuilder stringBuilder = new StringBuilder();
                if (!faceInfoList.isEmpty()) {
                    stringBuilder.append("face info:\n\n");
                }
                for (int i = 0; i < faceInfoList.size(); i++) {
                    stringBuilder.append("face[")
                            .append(i)
                            .append("]:\n")
                            .append(faceInfoList.get(i))
                            .append("\nage:")
                            .append(ageInfoList.get(i).getAge())
                            .append("\ngender:")
                            .append(genderInfoList.get(i).getGender() == GenderInfo.MALE ? "MALE"
                                    : (genderInfoList.get(i).getGender() == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN"))
                            .append("\nmaskInfo:")
                            .append(maskInfoList.get(i).getMask() == MaskInfo.WORN ? "Mask"
                                    : (maskInfoList.get(i).getMask() == MaskInfo.NOT_WORN ? "No Mask" : "UNKNOWN"))
                            .append("\n\n");
                }
                tvMainImageInfo.setText(stringBuilder);
            } else if (type == TYPE_ITEM) {
                FaceFeature faceFeature = new FaceFeature();
                int res = mainFaceEngine.extractFaceFeature(bgr24, width, height, FaceSDK.CP_PAF_BGR24, faceInfoList.get(0),
                        ExtractType.RECOGNIZE, isMask, faceFeature);
                if (res == 0) {
                    FaceSimilar faceSimilar = new FaceSimilar();
                    int compareResult = mainFaceEngine.compareFaceFeature(mainFeature, faceFeature, faceSimilar);
                    if (compareResult == ErrorInfo.MOK) {
                        ItemShowInfo showInfo = new ItemShowInfo(bitmap, ageInfoList.get(0).getAge(), genderInfoList.get(0).getGender(), faceSimilar.getScore());
                        showInfoList.add(showInfo);
                        multiFaceInfoAdapter.notifyItemInserted(showInfoList.size() - 1);
                    } else {
                        showToast(getString(R.string.compare_failed, compareResult));
                    }
                }
            }
        } else {
            if (type == TYPE_MAIN) {
                mainBitmap = null;
            }
        }
    }

    public void chooseLocalImage(int action) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, action);
    }

    public void addItemFace(View view) {
        if (faceEngineCode != ErrorInfo.MOK) {
            showToast(getString(R.string.engine_not_initialized, faceEngineCode));
            return;
        }
        if (mainBitmap == null) {
            showToast(getString(R.string.notice_choose_main_img));
            return;
        }
        chooseLocalImage(ACTION_ADD_RECYCLER_ITEM_IMAGE);
    }

    public void chooseMainImage(View view) {

        if (faceEngineCode != ErrorInfo.MOK) {
            showToast(getString(R.string.engine_not_initialized, faceEngineCode));
            return;
        }
        chooseLocalImage(ACTION_CHOOSE_MAIN_IMAGE);
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

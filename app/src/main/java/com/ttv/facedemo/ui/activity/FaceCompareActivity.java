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

import com.ttv.face.FaceEngine;
import com.ttv.face.FaceResult;
import com.ttv.facedemo.R;
import com.ttv.facedemo.ui.adapter.MultiFaceInfoAdapter;
import com.ttv.facedemo.ui.model.ItemShowInfo;
import com.ttv.face.GenderInfo;
import com.ttv.face.MaskInfo;
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

    private ImageView ivMainImage;
    private TextView tvMainImageInfo;


    private static final int TYPE_MAIN = 0;
    private static final int TYPE_ITEM = 1;

    private byte[] mainFeature;
    private MultiFaceInfoAdapter multiFaceInfoAdapter;
    private List<ItemShowInfo> showInfoList;
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
    }

    private void unInitEngine() {
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

        bitmap = TTVImageUtil.getAlignedBitmap(bitmap, true);
        if (bitmap == null) {
            return;
        }
        byte[] bgr24 = TTVImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), TTVImageFormat.BGR24);
        int transformCode = TTVImageUtil.bitmapToImageData(bitmap, bgr24, TTVImageFormat.BGR24);
        if (transformCode != TTVImageUtilError.CODE_SUCCESS) {
            showToast("failed to transform bitmap to imageData, code is " + transformCode);
            return;
        }
        List<FaceResult> faceInfoList = new ArrayList<>();
        faceInfoList = FaceEngine.getInstance(this).detectFace(bitmap);

        Bitmap bitmap565 = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmap565);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        if (!faceInfoList.isEmpty()) {
            for (int i = 0; i < faceInfoList.size(); i++) {
                 paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(faceInfoList.get(i).rect, paint);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setTextSize((float) faceInfoList.get(i).rect.width() / 2);
                canvas.drawText("" + i, faceInfoList.get(i).rect.left, faceInfoList.get(i).rect.top, paint);
            }
        }


        FaceEngine.getInstance(this).faceAttrProcess(bitmap, faceInfoList);
        int isMask = MaskInfo.UNKNOWN;
        if (!faceInfoList.isEmpty()) {
            isMask = faceInfoList.get(0).mask;
        }

        if (isMask == MaskInfo.UNKNOWN) {
            showToast("mask is unknown");
            return;
        }

        if (type == TYPE_MAIN && isMask == MaskInfo.WORN) {
            showToast(getResources().getString(R.string.notice_register_image_no_mask));
            return;
        }

        if (!faceInfoList.isEmpty()) {
            if (type == TYPE_MAIN) {
                int size = showInfoList.size();
                showInfoList.clear();
                multiFaceInfoAdapter.notifyItemRangeRemoved(0, size);
                mainFeature = null;
                FaceEngine.getInstance(this).extractFeature(bitmap, true, faceInfoList);

                if(faceInfoList.size() > 0)
                    mainFeature = faceInfoList.get(0).feature;

                Glide.with(ivMainImage.getContext())
                        .load(bitmap)
                        .into(ivMainImage);
                StringBuilder stringBuilder = new StringBuilder();
                if (!faceInfoList.isEmpty()) {
                    stringBuilder.append("face info:\n\n");
                }
                for (int i = 0; i < faceInfoList.size(); i++) {

                    String angleInfo = "Face3DAngle{yaw=" + faceInfoList.get(i).yaw + ", roll=" + faceInfoList.get(i).roll + ", pitch=" + faceInfoList.get(i).pitch + '}';
                    String faceInfo = "FaceInfo{faceRect=" + faceInfoList.get(i).rect.toString() + ", orient=" + faceInfoList.get(i).orient + ", " + angleInfo + '}';

                    stringBuilder.append("face[")
                            .append(i)
                            .append("]:\n")
                            .append(faceInfo)
                            .append("\nage:")
                            .append(faceInfoList.get(i).age)
                            .append("\ngender:")
                            .append(faceInfoList.get(i).gender == GenderInfo.MALE ? "MALE"
                                    : (faceInfoList.get(i).gender == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN"))
                            .append("\nmaskInfo:")
                            .append(faceInfoList.get(i).mask == MaskInfo.WORN ? "Mask"
                                    : (faceInfoList.get(i).mask == MaskInfo.NOT_WORN ? "No Mask" : "UNKNOWN"))
                            .append("\n\n");
                }
                tvMainImageInfo.setText(stringBuilder);
            } else if (type == TYPE_ITEM) {
                FaceEngine.getInstance(this).extractFeature(bitmap, false, faceInfoList);
                if(faceInfoList.size() > 0) {
                    float score = FaceEngine.getInstance(this).compareFeature(mainFeature, faceInfoList.get(0).feature);
                    ItemShowInfo showInfo = new ItemShowInfo(bitmap, faceInfoList.get(0).age, faceInfoList.get(0).gender, score);
                    showInfoList.add(showInfo);
                    multiFaceInfoAdapter.notifyItemInserted(showInfoList.size() - 1);
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
        if (mainBitmap == null) {
            showToast(getString(R.string.notice_choose_main_img));
            return;
        }
        chooseLocalImage(ACTION_ADD_RECYCLER_ITEM_IMAGE);
    }

    public void chooseMainImage(View view) {
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

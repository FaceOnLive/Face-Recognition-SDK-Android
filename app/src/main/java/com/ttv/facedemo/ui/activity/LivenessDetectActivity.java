package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.ttv.facedemo.R;
import com.ttv.facedemo.databinding.ActivityLivenessDetectBinding;
import com.ttv.facedemo.ui.model.PreviewConfig;
import com.ttv.facedemo.ui.viewmodel.LivenessDetectViewModel;
import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.facedemo.util.FaceRectTransformer;
import com.ttv.facedemo.util.camera.CameraListener;
import com.ttv.facedemo.util.camera.DualCameraHelper;
import com.ttv.facedemo.util.face.constants.LivenessType;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.facedemo.widget.FaceRectView;

import java.util.List;

public class LivenessDetectActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "LivenessDetectActivity";
    private DualCameraHelper rgbCameraHelper;
    private DualCameraHelper irCameraHelper;
    private FaceRectTransformer rgbFaceRectTransformer;
    private FaceRectTransformer irFaceRectTransformer;

    private PreviewConfig previewConfig;

    private LivenessType livenessType;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA

    };
    private ActivityLivenessDetectBinding binding;

    private LivenessDetectViewModel livenessDetectViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_liveness_detect);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        initModel();
        initView();
        initViewModel();
    }


    private void initModel() {
        boolean switchCamera = ConfigUtil.isSwitchCamera(this);
        previewConfig = new PreviewConfig(
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK,
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT,
                Integer.parseInt(ConfigUtil.getRgbCameraAdditionalRotation(this)),
                Integer.parseInt(ConfigUtil.getIrCameraAdditionalRotation(this))
        );
        String livenessTypeStr = ConfigUtil.getLivenessDetectType(this);
        if (livenessTypeStr.equals((getString(R.string.value_liveness_type_rgb)))) {
            livenessType = LivenessType.RGB;
        } else if (livenessTypeStr.equals(getString(R.string.value_liveness_type_ir))) {
            livenessType = LivenessType.IR;
        } else {
            livenessType = null;
        }
    }

    private void initViewModel() {
        livenessDetectViewModel = new ViewModelProvider(
                getViewModelStore(),
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        )
                .get(LivenessDetectViewModel.class);
        livenessDetectViewModel.init(DualCameraHelper.canOpenDualCamera());
    }


    private void initView() {
        if (!DualCameraHelper.hasDualCamera() || livenessType != LivenessType.IR) {
            binding.flRecognizeIr.setVisibility(View.GONE);
        }
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDestroy() {
        if (irCameraHelper != null) {
            irCameraHelper.release();
            irCameraHelper = null;
        }

        if (rgbCameraHelper != null) {
            rgbCameraHelper.release();
            rgbCameraHelper = null;
        }

        livenessDetectViewModel.destroy();
        super.onDestroy();
    }

    private ViewGroup.LayoutParams adjustPreviewViewSize(View rgbPreview, View previewView, FaceRectView faceRectView, Camera.Size previewSize, int displayOrientation, float scale) {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        int measuredWidth = previewView.getMeasuredWidth();
        int measuredHeight = previewView.getMeasuredHeight();
        float ratio = ((float) previewSize.height) / (float) previewSize.width;
        if (ratio > 1) {
            ratio = 1 / ratio;
        }
        if (displayOrientation % 180 == 0) {
            layoutParams.width = measuredWidth;
            layoutParams.height = (int) (measuredWidth * ratio);
        } else {
            layoutParams.height = measuredHeight;
            layoutParams.width = (int) (measuredHeight * ratio);
        }
        if (scale < 1f) {
            ViewGroup.LayoutParams rgbParam = rgbPreview.getLayoutParams();
            layoutParams.width = (int) (rgbParam.width * scale);
            layoutParams.height = (int) (rgbParam.height * scale);
        } else {
            layoutParams.width *= scale;
            layoutParams.height *= scale;
        }


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (layoutParams.width >= metrics.widthPixels) {
            float viewRatio = layoutParams.width / ((float) metrics.widthPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }
        if (layoutParams.height >= metrics.heightPixels) {
            float viewRatio = layoutParams.height / ((float) metrics.heightPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }


        previewView.setLayoutParams(layoutParams);
        faceRectView.setLayoutParams(layoutParams);
        return layoutParams;
    }

    private void initRgbCamera() {
        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size previewSizeRgb = camera.getParameters().getPreviewSize();
                ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(binding.dualCameraTexturePreviewRgb,
                        binding.dualCameraTexturePreviewRgb, binding.dualCameraFaceRectView,
                        previewSizeRgb, displayOrientation, 1.0f);
                rgbFaceRectTransformer = new FaceRectTransformer(
                        previewSizeRgb.width, previewSizeRgb.height,
                        layoutParams.width, layoutParams.height,
                        displayOrientation, cameraId, isMirror,
                        ConfigUtil.isDrawRgbRectHorizontalMirror(LivenessDetectActivity.this),
                        ConfigUtil.isDrawRgbRectVerticalMirror(LivenessDetectActivity.this)
                );

                livenessDetectViewModel.onRgbCameraOpened(camera);
                livenessDetectViewModel.setRgbFaceRectTransformer(rgbFaceRectTransformer);
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                binding.dualCameraFaceRectView.clearFaceInfo();
                binding.dualCameraFaceRectViewIr.clearFaceInfo();
                List<FacePreviewInfo> facePreviewInfoList = livenessDetectViewModel.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && rgbFaceRectTransformer != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (rgbFaceRectTransformer != null) {
                    rgbFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        rgbCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewRgb.getMeasuredWidth(), binding.dualCameraTexturePreviewRgb.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(previewConfig.getRgbCameraId())
                .isMirror(ConfigUtil.isDrawRgbPreviewHorizontalMirror(this))
                .additionalRotation(Integer.parseInt(ConfigUtil.getRgbCameraAdditionalRotation(this)))
                .previewSize(livenessDetectViewModel.loadPreviewSize())
                .previewOn(binding.dualCameraTexturePreviewRgb)
                .cameraListener(cameraListener)
                .build();
        rgbCameraHelper.init();
        rgbCameraHelper.start();
    }


    private void initIrCamera() {
        CameraListener irCameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size previewSizeIr = camera.getParameters().getPreviewSize();
                ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(binding.dualCameraTexturePreviewRgb,
                        binding.dualCameraTexturePreviewIr, binding.dualCameraFaceRectViewIr,
                        previewSizeIr, displayOrientation, 0.25f);
                irFaceRectTransformer = new FaceRectTransformer(
                        previewSizeIr.width, previewSizeIr.height,
                        layoutParams.width, layoutParams.height,
                        displayOrientation, cameraId, isMirror,
                        ConfigUtil.isDrawIrRectHorizontalMirror(LivenessDetectActivity.this),
                        ConfigUtil.isDrawIrRectVerticalMirror(LivenessDetectActivity.this)
                );
                TextView textViewIr = new TextView(LivenessDetectActivity.this, null);
                textViewIr.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textViewIr.setText(getString(R.string.camera_ir_preview_size, previewSizeIr.width, previewSizeIr.height));
                textViewIr.setTextColor(Color.WHITE);
                textViewIr.setBackgroundColor(getResources().getColor(R.color.color_bg_notification));
                ((FrameLayout) binding.dualCameraTexturePreviewIr.getParent()).addView(textViewIr);
                livenessDetectViewModel.onIrCameraOpened(camera);
                livenessDetectViewModel.setIrFaceRectTransformer(irFaceRectTransformer);
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                livenessDetectViewModel.refreshIrPreviewData(nv21);
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (irFaceRectTransformer != null) {
                    irFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        irCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewIr.getMeasuredWidth(), binding.dualCameraTexturePreviewIr.getMeasuredHeight()))
                .previewSize(livenessDetectViewModel.loadPreviewSize())
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .additionalRotation(Integer.parseInt(ConfigUtil.getIrCameraAdditionalRotation(this)))
                .specificCameraId(previewConfig.getIrCameraId())
                .previewOn(binding.dualCameraTexturePreviewIr)
                .cameraListener(irCameraListener)
                .isMirror(ConfigUtil.isDrawIrPreviewHorizontalMirror(this))
                .build();
        irCameraHelper.init();
        try {
            irCameraHelper.start();
        } catch (RuntimeException e) {
            showToast(e.getMessage() + getString(R.string.camera_error_notice));
        }
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        if (rgbFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> rgbDrawInfoList = livenessDetectViewModel.getDrawInfo(facePreviewInfoList, LivenessType.RGB);
            binding.dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList);
        }
        if (irFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> irDrawInfoList = livenessDetectViewModel.getDrawInfo(facePreviewInfoList, LivenessType.IR);
            binding.dualCameraFaceRectViewIr.drawRealtimeFaceInfo(irDrawInfoList);
        }
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initRgbCamera();
                if (DualCameraHelper.hasDualCamera() && livenessType == LivenessType.IR) {
                    initIrCamera();
                }
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    public void switchCamera(View view) {
        try {
            if (rgbCameraHelper != null && irCameraHelper != null) {
                rgbCameraHelper.stop();
                irCameraHelper.stop();
                rgbCameraHelper.switchCameraId();
                irCameraHelper.switchCameraId();
                rgbCameraHelper.start();
                irCameraHelper.start();
                showLongToast(getString(R.string.notice_change_detect_degree));
            } else {
                showToast(getString(R.string.switch_camera_failed));
            }
        } catch (RuntimeException e) {
            showToast(e.getMessage() + getString(R.string.camera_error_notice));
        }
    }

    @Override
    public void onGlobalLayout() {
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initRgbCamera();
            if (DualCameraHelper.hasDualCamera() && livenessType == LivenessType.IR) {
                initIrCamera();
            }
        }
    }

    public void setting(View view) {
        navigateToNewPage(RecognizeSettingsActivity.class);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeCamera();
    }

    private void resumeCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.start();
        }
        if (irCameraHelper != null) {
            irCameraHelper.start();
        }
    }

    @Override
    protected void onPause() {
        pauseCamera();
        super.onPause();
    }

    private void pauseCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.stop();
        }
        if (irCameraHelper != null) {
            irCameraHelper.stop();
        }
    }
}

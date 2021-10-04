package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.ttv.facedemo.TTVFaceApplication;
import com.ttv.facedemo.R;
import com.ttv.facedemo.databinding.ActivityCameraConfigureBinding;
import com.ttv.facedemo.ui.model.PreviewConfig;
import com.ttv.facedemo.util.ConfigUtil;
import com.ttv.facedemo.util.ErrorCodeUtil;
import com.ttv.facedemo.util.FaceRectTransformer;
import com.ttv.facedemo.util.camera.CameraListener;
import com.ttv.facedemo.util.camera.DualCameraHelper;
import com.ttv.facedemo.util.camera.glsurface.CameraGLSurfaceView;
import com.ttv.facedemo.util.face.FaceHelper;
import com.ttv.facedemo.util.face.IDualCameraFaceInfoTransformer;
import com.ttv.facedemo.util.face.model.FacePreviewInfo;
import com.ttv.facedemo.util.face.model.RecognizeConfiguration;
import com.ttv.facedemo.widget.FaceRectView;
import com.ttv.face.AgeInfo;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.face.FaceInfo;
import com.ttv.face.GenderInfo;
import com.ttv.face.LivenessInfo;
import com.ttv.face.enums.DetectMode;

import java.util.ArrayList;
import java.util.List;

public class CameraConfigureActivity extends BaseActivity {
    private ActivityCameraConfigureBinding binding;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    private static final String TAG = "CameraConfigureActivity";

    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
    private CameraPreferenceFragment cameraConfigPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera_configure);
        initData();
        initView();
    }

    private void initData() {
    }

    private void initView() {
        if (!DualCameraHelper.hasDualCamera()) {
            binding.flIrPreview.setVisibility(View.GONE);
            binding.glSurfaceViewIr.setVisibility(View.GONE);
        }
        cameraConfigPreferenceFragment = new CameraPreferenceFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fl_camera_config_container, cameraConfigPreferenceFragment)
                .commit();
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public static class CameraPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private String switchCamera; //keep

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            CameraConfigureActivity cameraConfigureActivity = (CameraConfigureActivity) getActivity();
            if (cameraConfigureActivity == null) {
                return false;
            }
            if (preference instanceof SwitchPreferenceCompat) {
                if (key.equals(switchCamera)) {

                }
            }
            return true;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference_camera, rootKey);

            switchCamera = getString(R.string.preference_switch_camera);

            findPreference(switchCamera).setOnPreferenceChangeListener(this::onPreferenceChange);
        }
    }
}

package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.ttv.facedemo.R;

public class RecognizeSettingsActivity extends BaseActivity {

    private static final int ACTION_REQUEST_CAMERA = 1;
    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize_settings);
        enableBackIfActionBarExists();
        if (checkPermissions(NEEDED_PERMISSIONS)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new CameraPreferenceFragment())
                    .commit();
        } else {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_CAMERA);
        }
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        super.afterRequestPermission(requestCode, isAllGranted);
        if (!isAllGranted) {
            showLongToast(getString(R.string.permission_denied));
            return;
        }
        if (requestCode == ACTION_REQUEST_CAMERA) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new CameraPreferenceFragment())
                    .commit();
        }
    }

    public static class CameraPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private String switchCamera; //keep

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            RecognizeSettingsActivity cameraConfigureActivity = (RecognizeSettingsActivity) getActivity();
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
package com.ttv.facerecog;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsDelegate {

    private static final int REQUEST_CODE = 10;
    private final Activity activity;

    public PermissionsDelegate(Activity activity) {
        this.activity = activity;
    }

    public boolean hasCameraPersmission() {
        return (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    public boolean hasPermissions() {
        int write = ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE);
        int read = ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE);
        int camera = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        return (/*phoneState == PackageManager.PERMISSION_GRANTED &&*/
                camera == PackageManager.PERMISSION_GRANTED);
    }

    public void requestPermissions() {
        if(ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            return;
        }

        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
            return;
        }
    }

    public boolean resultGranted(int requestCode,
                                 String[] permissions,
                                 int[] grantResults) {

        if (requestCode != REQUEST_CODE) {
            return false;
        }

        if(hasPermissions())
            return true;

        requestPermissions();
        return false;
    }
}

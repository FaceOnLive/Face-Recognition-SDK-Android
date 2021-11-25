package com.ttv.facedemo.ui.activity;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceEngine;
import com.ttv.facedemo.R;
import com.ttv.facedemo.common.Base;
import com.ttv.facedemo.databinding.ActivityHomeBinding;
import com.ttv.facedemo.widget.NavigateItemView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import qrcode.QrCodeActivity;

public class HomeActivity extends BaseActivity  {
    private ActivityHomeBinding activityHomeBinding;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityHomeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        initView();

        String license = "";
        try {
            license = Base.getStringFromFile(Base.getAppDir(this) + "/license.txt");
        } catch (Exception e){}

        int activated = FaceEngine.getInstance(this).setActivation(license);
        Log.e(TAG, "activation: " + activated);
        if(activated != ErrorInfo.MOK) {
            Intent intent = new Intent(this, ActivationActivity.class);
            startActivity(intent);
            finish();
        }

        FaceEngine.getInstance(this).init(1);
    }

    private void initView() {
        activityHomeBinding.llOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, RegisterAndRecognizeActivity.class));
            }
        });

        activityHomeBinding.llTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, LivenessDetectActivity.class));
            }
        });


        activityHomeBinding.llThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ImageFaceAttrDetectActivity.class));
            }
        });

        activityHomeBinding.llFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, FaceCompareActivity.class));
            }
        });


        activityHomeBinding.llFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, FaceManageActivity.class));
            }
        });

        activityHomeBinding.llSix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, RecognizeSettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (permission()) {
            openNewScreen();
        } else {
            RequestPermission_Dialog();
        }
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (isAllGranted) {
        } else {
            showToast(getString(R.string.permission_denied));
        }
    }

    public boolean permission() {
/*        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {*/
            int write = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        //}
    }

    private void openNewScreen() {

    }

    public void RequestPermission_Dialog() {
//        if (SDK_INT >= Build.VERSION_CODES.R) {
//            try {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                intent.addCategory("android.intent.category.DEFAULT");
//                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
//                startActivityForResult(intent, 2000);
//            } catch (Exception e) {
//                Intent obj = new Intent();
//                obj.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(obj, 2000);
//            }
//        } else {
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    boolean storage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (storage && read) {
                        openNewScreen();
                    } else {
                        //permission denied
                    }
                }
                break;
        }
    }
}

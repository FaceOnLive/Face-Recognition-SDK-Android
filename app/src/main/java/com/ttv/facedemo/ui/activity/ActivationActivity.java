package com.ttv.facedemo.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ttv.face.ErrorInfo;
import com.ttv.face.FaceSDK;
import com.ttv.facedemo.R;
import com.ttv.facedemo.common.Base;
import com.ttv.facedemo.common.FileChooser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import qrcode.QrCodeActivity;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

public class ActivationActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "ActivationActivity";

    final static int REQUEST_CODE = 3333;

    private EditText mEditHWID;
    private String mHWID;
    private File mLastFile;
    private Context mContext;
    private FaceSDK mFaceSDK = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        mContext = this;
        ((Button) findViewById(R.id.btnHWID)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnLicense)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnScan)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnSendEmail)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnSetActivate)).setOnClickListener(this);


        mFaceSDK = new FaceSDK(this);
        mHWID = mFaceSDK.getCurrentHWID();
        mEditHWID = (EditText) findViewById(R.id.editHWID);

        mEditHWID.setText(mHWID);

        updateQRCode();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (permission()) {
            openNewScreen();
        } else {
            RequestPermission_Dialog();
        }
    }

    public boolean permission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openNewScreen() {

    }

    public void RequestPermission_Dialog() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                startActivityForResult(intent, 2000);
            } catch (Exception e) {
                Intent obj = new Intent();
                obj.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(obj, 2000);
            }
        } else {
            ActivityCompat.requestPermissions(ActivationActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
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
            case 1:
                if (grantResults.length > 0) {
                    boolean storage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storage) {
                        Intent intent = new Intent(this, QrCodeActivity.class);
                        startActivityForResult(intent, 0);
                    } else {
                        //permission denied
                    }
                }
                break;
        }
    }

    private int askForPermission(String permission, Integer requestCode) {

        if (ContextCompat.checkSelfPermission(ActivationActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(ActivationActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(ActivationActivity.this, new String[]{permission}, requestCode);
            } else {

                ActivityCompat.requestPermissions(ActivationActivity.this, new String[]{permission}, requestCode);
            }
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnHWID: {
                FileChooser fileChooser = new FileChooser(ActivationActivity.this, "Select folder", FileChooser.DialogType.SELECT_DIRECTORY, mLastFile);
                FileChooser.FileSelectionCallback callback = new FileChooser.FileSelectionCallback() {

                    @Override
                    public void onSelect(File file) {
                        //Do something with the selected file
                        Log.e(TAG, "file path: " + file.getPath());
                        mLastFile = file;

                        try {
                            Base.saveStringToFile(mContext, file.getPath() + "/hwid.txt", mHWID);
                            Toast.makeText(getBaseContext(), "File saved successfully!",
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                fileChooser.show(callback);

                break;
            }
            case R.id.btnLicense: {
                FileChooser fileChooser = new FileChooser(ActivationActivity.this, "Select license file", FileChooser.DialogType.SELECT_FILE, mLastFile);
                FileChooser.FileSelectionCallback callback = new FileChooser.FileSelectionCallback() {

                    @Override
                    public void onSelect(File file) {
                        //Do something with the selected file
                        Log.e(TAG, "file path: " + file.getPath());
                        mLastFile = file;

                        try {
                            String licenseStr = Base.getStringFromFile(file.getPath());
                            Log.e(TAG, "licenseStr: " + licenseStr);

                            int activated = mFaceSDK.setActivation(licenseStr);
                            Log.e(TAG, "setActivation: " + activated);

                            if (activated != ErrorInfo.MOK) {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
                                alertBuilder.setTitle("Warning").setMessage("Activation Failed!").setPositiveButton(android.R.string.ok, null).show();
                            } else {
                                Base.saveStringToFile(mContext, Base.getAppDir(mContext) + "/license.txt", licenseStr);
                                Intent intent = new Intent(mContext, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                fileChooser.show(callback);

                break;
            }
            case R.id.btnScan: {
                if(askForPermission(Manifest.permission.CAMERA, 1) == 1) {
                    Intent intent = new Intent(this, QrCodeActivity.class);
                    startActivityForResult(intent, 0);
                }
                break;
            }
            case R.id.btnSendEmail: {
                Intent intent = new Intent(Intent.ACTION_SEND);

                String strExportTitle = "Get License";

                intent.putExtra(Intent.EXTRA_SUBJECT, strExportTitle);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, mHWID);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"turing311@outlook.com"});
                startActivity(Intent.createChooser(intent, "send to email"));
                break;
            }
            case R.id.btnSetActivate: {
                try {
                    EditText editLicense = (EditText) findViewById(R.id.editLicense);
                    String licenseStr = editLicense.getText().toString();
                    Log.e(TAG, "licenseStr: " + licenseStr);

                    int activated = mFaceSDK.setActivation(licenseStr);
                    Log.e(TAG, "setActivation: " + activated);

                    if (activated != ErrorInfo.MOK) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
                        alertBuilder.setTitle("Warning").setMessage("Activation Failed!").setPositiveButton(android.R.string.ok, null).show();
                    } else {
                        Base.saveStringToFile(mContext, Base.getAppDir(mContext) + "/license.txt", licenseStr);
                        Intent intent = new Intent(mContext, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void updateQRCode() {
        if(mHWID == null)
            return;

        com.google.zxing.Writer writer = new QRCodeWriter();
        try {
            BitMatrix bm = writer.encode(mHWID, BarcodeFormat.QR_CODE, 800, 800);
            Bitmap qrcodeBmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < 800; i++) {
                for (int j = 0; j < 800; j++) {
                    qrcodeBmp.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

//            ((ImageView) findViewById(R.id.hwid_qrcode_view)).setImageBitmap(qrcodeBmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "requestCode = " + requestCode + " resultCode = " + resultCode);
        switch (requestCode) {
            case 0: {
                if (resultCode == RESULT_OK) {
                    try {
                        String licenseStr = data.getExtras().getString("Result");
                        int activated = mFaceSDK.setActivation(licenseStr);
                        Log.e(TAG, "setActivation: " + activated);

                        if (activated != ErrorInfo.MOK) {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
                            alertBuilder.setTitle("Warning").setMessage("Activation Failed!").setPositiveButton(android.R.string.ok, null).show();
                        } else {
                            Base.saveStringToFile(mContext, Base.getAppDir(mContext) + "/license.txt", licenseStr);
                            Intent intent = new Intent(mContext, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

}

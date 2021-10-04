package com.ttv.facedemo.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ttv.facedemo.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseActivity extends AppCompatActivity {
    private static ExecutorService executor;
    private static List<Activity> activityList;

    protected static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final int ACTION_CHOOSE_IMAGE = 0x201;
    private static final int SNACK_BAR_MAX_LINES = 50;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (activityList == null) {
            activityList = new ArrayList<>();
        }
        if (executor == null) {
            executor = new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("activity-sub-thread-" + t.getId());
                        return t;
                    });
        }
        activityList.add(this);
    }

    public void runOnSubThread(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    protected void onDestroy() {
        activityList.remove(this);
        if (activityList.isEmpty()) {
            activityList = null;
            executor.shutdown();
            executor = null;
        }

        super.onDestroy();
    }

    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGranted = true;
        for (int grantResult : grantResults) {
            isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
        }
        afterRequestPermission(requestCode, isAllGranted);
    }

    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }

    public void navigateToNewPage(Class activityClass) {
        startActivity(new Intent(this, activityClass));
        overridePendingTransition(R.anim.activity_new_in, R.anim.activity_old_leave);
    }


    public void navigateToNewPageForResult(Class activityClass, int reqCode) {
        startActivityForResult(new Intent(this, activityClass), reqCode);
        overridePendingTransition(R.anim.activity_new_in, R.anim.activity_old_leave);
    }

    public void navigateToNewPage(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.activity_new_in, R.anim.activity_old_leave);
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_old_in, R.anim.activity_new_leave);
    }

    protected void showToast(final String s) {
        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toast.show();
        } else {
            runOnUiThread(toast::show);
        }
    }

    protected void showLongToast(final String s) {
        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toast.show();
        } else {
            runOnUiThread(toast::show);
        }
    }

    protected void showSnackBar(final View view, final String s) {
        Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_SHORT);
        enableSnackBarShowMultiLines(snackbar, SNACK_BAR_MAX_LINES);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            snackbar.show();
        } else {
            runOnUiThread(snackbar::show);
        }
    }

    protected void showLongSnackBar(final View view, final String s) {
        Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
        enableSnackBarShowMultiLines(snackbar, SNACK_BAR_MAX_LINES);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            snackbar.show();
        } else {
            runOnUiThread(snackbar::show);
        }
    }

    protected Snackbar showIndefiniteSnackBar(final View view, final String s, String action, View.OnClickListener onClickListener) {
        Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_INDEFINITE);
        enableSnackBarShowMultiLines(snackbar, SNACK_BAR_MAX_LINES);
        snackbar.setAction(action, onClickListener);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            snackbar.show();
        } else {
            runOnUiThread(snackbar::show);
        }
        return snackbar;
    }

    private void enableSnackBarShowMultiLines(Snackbar snackbar, int maxLines) {
        final SnackbarContentLayout contentLayout = (SnackbarContentLayout) ((ViewGroup) snackbar.getView()).getChildAt(0);
        final TextView tv = contentLayout.getMessageView();
        tv.setMaxLines(maxLines);
    }


    protected void getImageFromAlbum(OnGetImageFromAlbumCallback callback) {
        this.onGetImageFromAlbumCallback = callback;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, ACTION_CHOOSE_IMAGE);
    }

    interface OnGetImageFromAlbumCallback {

        void onGetImageFromAlbumSuccess(Uri imageUri);

        void onGetImageFromAlbumFailed();

    }

    OnGetImageFromAlbumCallback onGetImageFromAlbumCallback;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_CHOOSE_IMAGE) {
            if (data == null || data.getData() == null) {
                if (onGetImageFromAlbumCallback != null) {
                    onGetImageFromAlbumCallback.onGetImageFromAlbumFailed();
                }
                return;
            }
            if (onGetImageFromAlbumCallback != null) {
                onGetImageFromAlbumCallback.onGetImageFromAlbumSuccess(data.getData());
            }
        }
    }

    private AlertDialog noticeDialog;

    public void showNoticeDialog(String title, String content) {
        if (noticeDialog != null && noticeDialog.isShowing()) {
            noticeDialog.dismiss();
        }
        noticeDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(R.string.ok, null)
                .create();
        noticeDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void enableBackIfActionBarExists() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

}

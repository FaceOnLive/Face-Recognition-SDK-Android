package com.ttv.facedemo.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.ttv.facedemo.R;
import com.ttv.facedemo.common.Constants;
import com.ttv.facedemo.databinding.ActivityFaceManageBinding;
import com.ttv.facedemo.facedb.entity.FaceEntity;
import com.ttv.facedemo.ui.callback.BatchRegisterCallback;
import com.ttv.facedemo.ui.viewmodel.FacePhotoViewModel;
import com.ttv.facedemo.util.ImageUtil;
import com.ttv.facedemo.widget.FacePhotoAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.LinkedList;

public class FaceManageActivity extends BaseActivity implements FacePhotoAdapter.OnItemChangedListener, BaseActivity.OnGetImageFromAlbumCallback {
    private static final String TAG = "FaceManageActivity";
    private ActivityFaceManageBinding binding;
    private FacePhotoViewModel facePhotoViewModel;
    private FacePhotoAdapter facePhotoAdapter;
    private ColorStateList originBackgroundTintList;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_manage);
        initView();
        initViewModel();
        initData();
    }

    private void initData() {
        binding.setHasFace(true);
        runOnSubThread(() -> {
            facePhotoViewModel.init();
            facePhotoViewModel.loadData(false);
        });
    }

    @Override
    protected void onDestroy() {
        facePhotoViewModel.release();
        facePhotoViewModel = null;
        super.onDestroy();
    }

    private void initViewModel() {
        facePhotoViewModel = new ViewModelProvider(
                getViewModelStore(),
                new ViewModelProvider.Factory() {
                    @NonNull
                    @Override
                    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                        if (modelClass == FacePhotoViewModel.class) {
                            return (T) new FacePhotoViewModel();
                        }
                        throw new IllegalArgumentException(modelClass.getName() + " is not " + FacePhotoViewModel.class.getName());
                    }
                }
        )
                .get(FacePhotoViewModel.class);


        facePhotoViewModel.getFaceEntityList().observe(FaceManageActivity.this, faceEntities -> {
            if (faceEntities != null) {
                facePhotoAdapter.submitList(new LinkedList<>(faceEntities));
                binding.setHasFace(faceEntities.size() > 0);
            } else {
                facePhotoAdapter.submitList(null);
                binding.setHasFace(false);
            }
        });
        facePhotoViewModel.getInitFinished().observe(FaceManageActivity.this, aBoolean -> {
            binding.fabAdd.setClickable(true);
            if (originBackgroundTintList != null) {
                binding.fabAdd.setBackgroundTintList(originBackgroundTintList);
            }
        });
    }


    private void initView() {
        setSupportActionBar(binding.toolbar);
        enableBackIfActionBarExists();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        binding.rvFacePhoto.setLayoutManager(layoutManager);
        binding.rvFacePhoto.setItemAnimator(new DefaultItemAnimator());

        facePhotoAdapter = new FacePhotoAdapter(this, this);
        binding.rvFacePhoto.setAdapter(facePhotoAdapter);
        binding.rvFacePhoto.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                runOnSubThread(() -> facePhotoViewModel.listScrolled(lastVisibleItem, totalItemCount));
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

            }
        });
        binding.fabAdd.setOnClickListener(v -> {
            AlertDialog addFacesDialog = new AlertDialog.Builder(FaceManageActivity.this)
                    .setTitle(R.string.label_face_manage)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(R.array.face_manage_operations, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                getImageFromAlbum(FaceManageActivity.this);
                                break;
                            case 1:
                                registerFromFile(new File(Constants.DEFAULT_REGISTER_FACES_DIR));
                                break;
                            case 2:
                                facePhotoViewModel.clearAllFaces();
                                break;
                            default:
                                break;
                        }
                    })
                    .create();
            addFacesDialog.show();
        });
        binding.fabAdd.setClickable(false);
        originBackgroundTintList = binding.fabAdd.getBackgroundTintList();
        ColorStateList grayColorStateList = new ColorStateList(new int[][]{{Color.GRAY}}, new int[]{Color.GRAY});
        binding.fabAdd.setBackgroundTintList(grayColorStateList);
    }


    @Override
    public void onFaceItemRemoved(int position, FaceEntity faceEntity) {
        runOnSubThread(() -> {
            facePhotoViewModel.deleteFace(faceEntity);
            showLongSnackBar(binding.fabAdd, getString(R.string.face_deleted));
        });
    }

    @Override
    public void onFaceItemUpdated(int position, FaceEntity faceEntity) {
        runOnSubThread(() -> facePhotoViewModel.updateFace(position, faceEntity));
    }

    @Override
    public void onGetImageFromAlbumSuccess(Uri uri) {
        Bitmap bitmap = ImageUtil.uriToScaledBitmap(this, uri, ImageUtil.DEFAULT_MAX_WIDTH, ImageUtil.DEFAULT_MAX_HEIGHT);
        if (bitmap != null) {
            Log.i(TAG, "onGetImageFromAlbumSuccess: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            facePhotoViewModel.registerFace(bitmap, (facePreviewInfo, success) -> {
                showLongSnackBar(binding.fabAdd, getString(success ? R.string.register_success : R.string.register_failed));
            });
        } else {
            showToast(getString(R.string.get_picture_failed));
        }
    }

    private void registerFromFile(File dir) {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Snackbar snackbar = showIndefiniteSnackBar(binding.fabAdd, getString(R.string.registering_please_wait), getString(R.string.stop), v -> {
            if (facePhotoViewModel != null && facePhotoViewModel.stopRegisterIfDoing()) {
                showLongSnackBar(binding.fabAdd, getString(R.string.stopped));
                runOnSubThread(() -> facePhotoViewModel.loadData(true));
            }
        });
        facePhotoViewModel.registerFromFile(getApplicationContext(), dir, new BatchRegisterCallback() {

            @Override
            public void onProcess(int current, int failed, int total) {
                runOnUiThread(() -> snackbar.setText(getString(R.string.register_progress, current, failed, total)));
            }

            @Override
            public void onFinish(int current, int failed, int total, String errMsg) {
                runOnSubThread(() -> facePhotoViewModel.loadData(true));
                if (errMsg != null) {
                    showLongToast(errMsg);
                }
                snackbar.dismiss();
            }
        });

    }

    @Override
    public void onGetImageFromAlbumFailed() {
        showToast(getString(R.string.get_picture_failed));
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                registerFromFile(new File(Constants.DEFAULT_REGISTER_FACES_DIR));
            } else {
                showLongToast(getString(R.string.permission_denied));
            }
        }
    }
}

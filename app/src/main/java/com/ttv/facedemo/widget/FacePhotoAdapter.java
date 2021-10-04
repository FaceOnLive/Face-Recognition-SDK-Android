package com.ttv.facedemo.widget;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ttv.facedemo.TTVFaceApplication;
import com.ttv.facedemo.R;
import com.ttv.facedemo.databinding.RecyclerItemFacePhotoBinding;
import com.ttv.facedemo.facedb.entity.FaceEntity;
import com.bumptech.glide.Glide;

public class FacePhotoAdapter extends ListAdapter<FaceEntity, FacePhotoAdapter.FacePhotoHolder> {

    public interface OnItemClickedListener {

        void onFaceItemClicked(int position, FaceEntity faceEntity);

        void onFaceItemLongClicked(int position, FaceEntity faceEntity);
    }

    public interface OnItemChangedListener {
        void onFaceItemRemoved(int position, FaceEntity faceEntity);

        void onFaceItemUpdated(int position, FaceEntity faceEntity);
    }

    private OnItemClickedListener onItemClickedListener;


    public FacePhotoAdapter(Context context, OnItemChangedListener onItemChangedListener) {
        super(new DiffUtil.ItemCallback<FaceEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull FaceEntity oldItem, @NonNull FaceEntity newItem) {
                return oldItem.getFaceId() == newItem.getFaceId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull FaceEntity oldItem, @NonNull FaceEntity newItem) {
                return oldItem.equals(newItem);
            }
        });
        onItemClickedListener = new OnItemClickedListener() {

            @Override
            public void onFaceItemClicked(int position, final FaceEntity faceEntity) {
                performChangeFaceEntity(position, faceEntity, context, onItemChangedListener);
            }

            @Override
            public void onFaceItemLongClicked(int position, final FaceEntity faceEntity) {
                performDeleteFaceEntity(position, faceEntity, context, onItemChangedListener);
            }
        };
    }

    private void performDeleteFaceEntity(int position, FaceEntity faceEntity, Context context, OnItemChangedListener onItemChangedListener) {
        ImageView imageView = new ImageView(context);
        int size = context.getResources().getDimensionPixelSize(R.dimen.item_image_size);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        Glide.with(context).load(faceEntity.getImagePath()).thumbnail(0.5f).into(imageView);


        TextView textView = new TextView(context);
        textView.setText(context.getString(R.string.notice));
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(Color.WHITE);

        AlertDialog confirmDeleteDialog = new AlertDialog.Builder(context)
                .setCustomTitle(textView)
                .setMessage(context.getString(R.string.dialog_delete_face_config, faceEntity.getUserName()))
                .setView(imageView)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (onItemChangedListener != null) {
                        onItemChangedListener.onFaceItemRemoved(position, faceEntity);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        confirmDeleteDialog.getWindow().setBackgroundDrawableResource(R.color.back600);

        confirmDeleteDialog.show();
    }

    private void performChangeFaceEntity(int position, FaceEntity faceEntity, Context context, OnItemChangedListener onItemChangedListener) {
        View inputView = LayoutInflater.from(context).inflate(R.layout.dialog_input_view, null, false);
        EditText editText = inputView.findViewById(R.id.et_user_name);
        ImageView ivHead = inputView.findViewById(R.id.iv_head);
        Glide.with(context).load(faceEntity.getImagePath()).thumbnail(0.5f).into(ivHead);
        editText.setText(faceEntity.getUserName());
        AlertDialog confirmUpdateDialog = new AlertDialog.Builder(context)

                .setView(inputView)
                .setPositiveButton(TTVFaceApplication.getApplication().getString(R.string.ok), null)
                .setNegativeButton(TTVFaceApplication.getApplication().getString(R.string.cancel), null)
                .create();
        confirmUpdateDialog.getWindow().setBackgroundDrawableResource(R.color.back600);
        confirmUpdateDialog.show();
        confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                v -> {
                    String s = editText.getText().toString();
                    if (TextUtils.isEmpty(s)) {
                        editText.setError(TTVFaceApplication.getApplication().getString(R.string.name_should_not_be_empty));
                        return;
                    }
                    FaceEntity newFaceEntity = new FaceEntity(faceEntity);
                    newFaceEntity.setUserName(s);
                    if (onItemChangedListener != null) {
                        onItemChangedListener.onFaceItemUpdated(position, newFaceEntity);
                    }
                    confirmUpdateDialog.cancel();
                });
    }


    @NonNull
    @Override
    public FacePhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return FacePhotoHolder.create(parent, onItemClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FacePhotoHolder holder, int position) {
        holder.binding.setFaceEntity(getItem(position));
    }


    static class FacePhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        RecyclerItemFacePhotoBinding binding;

        OnItemClickedListener onItemClickedListener;

        static FacePhotoHolder create(ViewGroup parentView, OnItemClickedListener onItemClickedListener) {
            LayoutInflater inflater = LayoutInflater.from(parentView.getContext());
            FacePhotoHolder facePhotoHolder = new FacePhotoHolder(DataBindingUtil.inflate(inflater, R.layout.recycler_item_face_photo, parentView, false));
            facePhotoHolder.onItemClickedListener = onItemClickedListener;
            return facePhotoHolder;
        }

        private FacePhotoHolder(@NonNull RecyclerItemFacePhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onItemClickedListener != null) {
                int pos = getLayoutPosition();
                onItemClickedListener.onFaceItemClicked(pos, binding.getFaceEntity());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (onItemClickedListener != null) {
                int pos = getLayoutPosition();
                onItemClickedListener.onFaceItemLongClicked(pos, binding.getFaceEntity());
            }
            return true;
        }
    }
}

package com.ttv.facedemo.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ttv.facedemo.R;
import com.ttv.facedemo.ui.model.CompareResult;
import com.ttv.facedemo.widget.FaceSearchResultAdapter;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;


public class BindingUtil {
    @BindingAdapter("imgPath")
    public static void setImagePath(ImageView imageView, String path) {
        Glide.with(imageView.getContext())
                .load(path)
                .into(imageView);
    }

    @BindingAdapter("compareResultList")
    public static void setCompareResultList(RecyclerView recyclerView, List<CompareResult> compareResultList) {
        Context context = recyclerView.getContext();
        FaceSearchResultAdapter adapter = new FaceSearchResultAdapter(compareResultList, context);
        recyclerView.setAdapter(adapter);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int spanCount = dm.widthPixels /
                (context.getResources().getDimensionPixelSize(R.dimen.item_head_image_padding) * 2 +
                        context.getResources().getDimensionPixelSize(R.dimen.item_image_size));
        recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private static final SimpleDateFormat REGISTER_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @BindingAdapter("date")
    public static void setDate(TextView textView, long date) {
        synchronized (REGISTER_DATE_FORMAT) {
            textView.setText(REGISTER_DATE_FORMAT.format(date));
        }
    }
}

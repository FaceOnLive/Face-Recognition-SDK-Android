package com.ttv.facedemo.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ttv.facedemo.R;


public class FaceCountNotificationView extends LinearLayout {
    private static final String TAG = "FaceCountNotification";
    private TextView totalCountTextView;
    private TextView currentCountTextView;

    public FaceCountNotificationView(Context context) {
        super(context, null);
    }

    public FaceCountNotificationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    private void initView(Context context) {
        int separateLineHeight = 2;
        int totalSize = getResources().getDimensionPixelSize(R.dimen.face_count_notification_size);

        int textViewHeight = (totalSize - separateLineHeight) / 2;

        LayoutParams textViewLayoutParams = new LayoutParams(totalSize, textViewHeight);

        setBackgroundColor(Color.GRAY);
        setOrientation(LinearLayout.VERTICAL);

        totalCountTextView = new TextView(context);
        currentCountTextView = new TextView(context);
        totalCountTextView.setTextColor(Color.BLACK);
        currentCountTextView.setTextColor(Color.BLACK);
        totalCountTextView.setGravity(Gravity.CENTER);
        currentCountTextView.setGravity(Gravity.CENTER);

        totalCountTextView.setLayoutParams(textViewLayoutParams);
        currentCountTextView.setLayoutParams(textViewLayoutParams);


        View lineView = new View(context);
        LayoutParams separatorLayoutParams = new LayoutParams(totalSize, separateLineHeight);
        separatorLayoutParams.gravity = Gravity.CENTER;
        lineView.setBackgroundColor(Color.DKGRAY);
        lineView.setLayoutParams(separatorLayoutParams);


        addView(currentCountTextView);
        addView(lineView);
        addView(totalCountTextView);

        totalCountTextView.setText("");
        currentCountTextView.setText("");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    Rect rect = new Rect(0, 0, totalSize, totalSize);
                    outline.setRoundRect(rect, totalSize / 2);
                }
            });
            setClipToOutline(true);
        }
    }

    public void refreshCurrentFaceCount(int current) {
        currentCountTextView.setText(String.valueOf(current));
    }

    public void refreshTotalFaceCount(int total) {
        Log.i(TAG, "refreshTotalFaceCount: " + total);
        totalCountTextView.setText(String.valueOf(total));
    }

}

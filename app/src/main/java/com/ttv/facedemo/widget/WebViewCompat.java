package com.ttv.facedemo.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

public class WebViewCompat extends WebView {

    public WebViewCompat(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public static Context getFixedContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return context.createConfigurationContext(new Configuration());
        }
        return context;
    }
}
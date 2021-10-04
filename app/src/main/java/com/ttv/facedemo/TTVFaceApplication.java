package com.ttv.facedemo;

import android.app.Application;
import java.io.File;

public class TTVFaceApplication extends Application {
    private static TTVFaceApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    @Override
    public void onTerminate() {
        application = null;
        super.onTerminate();
    }

    public static TTVFaceApplication getApplication() {
        return application;
    }
}

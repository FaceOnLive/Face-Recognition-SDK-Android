package com.ttv.facedemo.util.camera.glsurface;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "CameraGLSurfaceView";


    YUVRenderer yuvRenderer;
    NV21Drawer nv21Drawer;

    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);

        yuvRenderer = new YUVRenderer();
        nv21Drawer = new NV21Drawer();
        setRenderer(yuvRenderer);

        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setFragmentShaderCode(String fragmentShaderCode) {
        nv21Drawer.setFragmentShaderCode(fragmentShaderCode);
    }

    public void init(boolean isMirror, int rotateDegree, int frameWidth, int frameHeight) {
        nv21Drawer.init(isMirror, rotateDegree, frameWidth, frameHeight);

        queueEvent(() -> yuvRenderer.initRenderer());
    }

    public class YUVRenderer implements Renderer {
        private void initRenderer() {
            boolean createSuccess = nv21Drawer.createGLProgram();
            if (!createSuccess) {
                Log.e(TAG, "initRenderer createGLProgram failed!");
            }
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            Log.i(TAG, "initRenderer onSurfaceCreated: ");
            initRenderer();
        }


        @Override
        public void onDrawFrame(GL10 gl) {
            nv21Drawer.render();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            Log.i(TAG, "onSurfaceChanged: ");
            GLES20.glViewport(0, 0, width, height);
        }
    }

    public void renderNV21(byte[] data) {
        nv21Drawer.updateNV21(data);
        requestRender();
    }


    public void renderNV21WithFaceRect(byte[] data, Rect faceRect, int strokeWidth) {
        nv21Drawer.updateNV21(data, faceRect, strokeWidth);
        requestRender();
    }
}

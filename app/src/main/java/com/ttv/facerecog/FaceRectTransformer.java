package com.ttv.facerecog;

import android.graphics.Rect;
import android.util.Log;


public class FaceRectTransformer {
    private int previewWidth, previewHeight, canvasWidth, canvasHeight, cameraDisplayOrientation;
    private boolean isMirror;
    private boolean mirrorHorizontal = false, mirrorVertical = false;
    private int cameraId;

    public FaceRectTransformer(int previewWidth, int previewHeight, int canvasWidth,
                               int canvasHeight, int cameraDisplayOrientation, int cameraId,
                               boolean isMirror, boolean mirrorHorizontal, boolean mirrorVertical) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.cameraDisplayOrientation = cameraDisplayOrientation;
        this.cameraId = cameraId;
        this.isMirror = isMirror;
        this.mirrorHorizontal = mirrorHorizontal;
        this.mirrorVertical = mirrorVertical;
    }

    public Rect adjustRect(Rect ftRect) {
        int previewWidth = this.previewWidth;
        int previewHeight = this.previewHeight;
        int canvasWidth = this.canvasWidth;
        int canvasHeight = this.canvasHeight;
        int cameraDisplayOrientation = this.cameraDisplayOrientation;
        int cameraId = this.cameraId;
        boolean isMirror = this.isMirror;
        boolean mirrorHorizontal = this.mirrorHorizontal;
        boolean mirrorVertical = this.mirrorVertical;

        if (ftRect == null) {
            return null;
        }

        Rect rect = new Rect(ftRect);
        float horizontalRatio;
        float verticalRatio;
        if (cameraDisplayOrientation % 180 == 0) {
            horizontalRatio = (float) canvasWidth / (float) previewWidth;
            verticalRatio = (float) canvasHeight / (float) previewHeight;
        } else {
            horizontalRatio = (float) canvasHeight / (float) previewWidth;
            verticalRatio = (float) canvasWidth / (float) previewHeight;
        }

        rect.left *= horizontalRatio;
        rect.right *= horizontalRatio;
        rect.top *= verticalRatio;
        rect.bottom *= verticalRatio;

        Rect newRect = new Rect();
        switch (cameraDisplayOrientation) {
            case 0:
                if (cameraId == 0) {
                    newRect.left = canvasWidth - rect.right;
                    newRect.right = canvasWidth - rect.left;
                } else {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                }
                newRect.top = rect.top;
                newRect.bottom = rect.bottom;
                break;
            case 90:
                newRect.right = canvasWidth - rect.top;
                newRect.left = canvasWidth - rect.bottom;
                if (cameraId == 0) {
                    newRect.top = canvasHeight - rect.right;
                    newRect.bottom = canvasHeight - rect.left;
                } else {
                    newRect.top = rect.left;
                    newRect.bottom = rect.right;
                }
                break;
            case 180:
                newRect.top = canvasHeight - rect.bottom;
                newRect.bottom = canvasHeight - rect.top;
                if (cameraId == 0) {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                } else {
                    newRect.left = canvasWidth - rect.right;
                    newRect.right = canvasWidth - rect.left;
                }
                break;
            case 270:
                newRect.left = rect.top;
                newRect.right = rect.bottom;
                if (cameraId == 0) {
                    newRect.top = rect.left;
                    newRect.bottom = rect.right;
                } else {
                    newRect.top = canvasHeight - rect.right;
                    newRect.bottom = canvasHeight - rect.left;
                }
                break;
            default:
                break;
        }

        /**
         * isMirror mirrorHorizontal finalIsMirrorHorizontal
         * true         true                false
         * false        false               false
         * true         false               true
         * false        true                true
         *
         * XOR
         */
        if (isMirror ^ mirrorHorizontal) {
            int left = newRect.left;
            int right = newRect.right;
            newRect.left = canvasWidth - right;
            newRect.right = canvasWidth - left;
        }
        if (mirrorVertical) {
            int top = newRect.top;
            int bottom = newRect.bottom;
            newRect.top = canvasHeight - bottom;
            newRect.bottom = canvasHeight - top;
        }
        return newRect;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public void setCanvasWidth(int canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    public void setCanvasHeight(int canvasHeight) {
        this.canvasHeight = canvasHeight;
    }

    public void setCameraDisplayOrientation(int cameraDisplayOrientation) {
        this.cameraDisplayOrientation = cameraDisplayOrientation;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public void setMirror(boolean mirror) {
        isMirror = mirror;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public int getCanvasWidth() {
        return canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public int getCameraDisplayOrientation() {
        return cameraDisplayOrientation;
    }

    public int getCameraId() {
        return cameraId;
    }

    public boolean isMirror() {
        return isMirror;
    }

    public boolean isMirrorHorizontal() {
        return mirrorHorizontal;
    }

    public void setMirrorHorizontal(boolean mirrorHorizontal) {
        this.mirrorHorizontal = mirrorHorizontal;
    }

    public boolean isMirrorVertical() {
        return mirrorVertical;
    }

    public void setMirrorVertical(boolean mirrorVertical) {
        this.mirrorVertical = mirrorVertical;
    }
}

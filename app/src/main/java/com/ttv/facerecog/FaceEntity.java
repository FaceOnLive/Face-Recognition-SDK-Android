package com.ttv.facerecog;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import com.ttv.face.FaceResult;

import java.io.IOException;
import java.io.InputStream;

public class FaceEntity {

    public String userName;
    public Bitmap headImg;
    public byte[] feature;
    public int user_id;

    public FaceEntity() {

    }

    public FaceEntity(int user_id, String userName, Bitmap headImg, byte[] feature) {
        this.user_id = user_id;
        this.userName = userName;
        this.headImg = headImg;
        this.feature = feature;
    }
}

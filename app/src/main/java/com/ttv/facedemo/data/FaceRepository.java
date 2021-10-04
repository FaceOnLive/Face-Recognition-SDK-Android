package com.ttv.facedemo.data;

import android.content.Context;
import android.util.Log;

import com.ttv.facedemo.facedb.db.FaceDB;
import com.ttv.facedemo.facedb.entity.FaceEntity;
import com.ttv.facedemo.facedb.FaceManager;
import com.ttv.facedemo.facedb.RegisterFailedException;

import java.io.File;
import java.util.List;

public class FaceRepository {
    private FaceDB faceDao;
    private int currentIndex = 0;
    private int pageSize;
    private static final String TAG = "FaceRepository";
    private FaceManager faceServer;

    public FaceRepository(int pageSize, FaceDB faceDao, FaceManager faceServer) {
        this.pageSize = pageSize;
        this.faceDao = faceDao;
        this.faceServer = faceServer;
    }

    public List<FaceEntity> loadMore() {
        List<FaceEntity> faceEntities = faceDao.getFaces(currentIndex, pageSize);
        currentIndex += faceEntities.size();
        return faceEntities;
    }

    public List<FaceEntity> reload() {
        currentIndex = 0;
        return loadMore();
    }

    public int clearAll() {
        int faceCount = faceServer.clearAllFaces();
        currentIndex = 0;
        return faceCount;
    }

    public int delete(FaceEntity faceEntity) {
        int index = faceDao.deleteFace(faceEntity);
        boolean delete = new File(faceEntity.getImagePath()).delete();
        if (!delete) {
            Log.w(TAG, "deleteFace: failed to delete headImageFile '" + faceEntity.getImagePath() + "'");
        }
        return index;
    }


    public FaceEntity registerJpeg(Context context, byte[] bytes, String name) throws RegisterFailedException {
        return faceServer.registerJpeg(context, bytes, name);
    }

    public FaceEntity registerBgr24(Context context, byte[] bgr24Data, int width, int height, String name) {
        return faceServer.registerBgr24(context, bgr24Data, width, height, name);
    }

    public int getTotalFaceCount() {
        return faceDao.getFaceCount();
    }
}

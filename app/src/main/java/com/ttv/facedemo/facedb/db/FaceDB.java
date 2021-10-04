package com.ttv.facedemo.facedb.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.ttv.facedemo.facedb.entity.FaceEntity;

import java.util.List;

@Dao
public interface FaceDB {
    @Query("SELECT * FROM face")
    List<FaceEntity> getAllFaces();

    @Query("SELECT * FROM face order by faceId desc limit :start,:size ")
    List<FaceEntity> getFaces(int start, int size);

    @Update
    int updateFaceEntity(FaceEntity faceEntity);

    @Delete
    int deleteFace(FaceEntity faceEntity);

    @Query("DELETE from face")
    int deleteAll();

    @Insert
    long insert(FaceEntity faceEntity);

    @Query("SELECT COUNT(1) from face")
    int getFaceCount();

    @Query("SELECT * FROM face WHERE faceId = :faceId limit 1")
    FaceEntity queryByFaceId(int faceId);
}

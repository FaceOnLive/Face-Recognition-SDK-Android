package com.ttv.facedemo.facedb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ttv.facedemo.facedb.db.FaceDB;
import com.ttv.facedemo.facedb.entity.FaceEntity;

import java.io.File;

@Database(entities = {FaceEntity.class}, version = 1, exportSchema = false)
public abstract class FaceDatabase extends RoomDatabase {
    public abstract FaceDB faceDao();

    private static volatile FaceDatabase faceDatabase = null;

    public static FaceDatabase getInstance(Context context) {
        if (faceDatabase == null) {
            synchronized (FaceDatabase.class) {
                if (faceDatabase == null) {
                    faceDatabase = Room.databaseBuilder(context, FaceDatabase.class,
                            context.getExternalFilesDir("database") + File.separator + "faceDB.db").build();
                }
            }
        }
        return faceDatabase;
    }
}

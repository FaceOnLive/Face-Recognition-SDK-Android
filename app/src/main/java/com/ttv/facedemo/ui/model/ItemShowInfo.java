package com.ttv.facedemo.ui.model;

import android.graphics.Bitmap;

import com.ttv.face.GenderInfo;


public class ItemShowInfo {
    private Bitmap bitmap;
    private int age;
    private int gender;
    private float similar;

    public ItemShowInfo() {
    }

    public ItemShowInfo(Bitmap bitmap, int age, int gender, float similar) {
        this.bitmap = bitmap;
        this.age = age;
        this.gender = gender;
        this.similar = similar;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }


    @Override
    public String toString() {
        return
                " age=" + age +
                        ", gender=" + (gender == GenderInfo.MALE ? "MALE" : (gender == GenderInfo.FEMALE ? "FEMALE" : "UNKNOWN")) +
                        ", similar=" + similar;
    }
}

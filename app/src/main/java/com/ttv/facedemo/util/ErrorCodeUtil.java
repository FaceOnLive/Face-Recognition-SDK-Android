package com.ttv.facedemo.util;

import com.ttv.face.ErrorInfo;
import com.ttv.imageutil.TTVImageUtilError;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ErrorCodeUtil {

    public static String ttvErrorCodeToFieldName(int code) {
        Field[] declaredFields = ErrorInfo.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            try {
                if (Modifier.isFinal(declaredField.getModifiers()) && ((int) declaredField.get(ErrorInfo.class)) == code) {
                    return declaredField.getName();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return "unknown error";
    }

    public static String imageUtilErrorCodeToFieldName(int code) {
        Field[] declaredFields = TTVImageUtilError.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            try {
                if (Modifier.isFinal(declaredField.getModifiers()) && ((int) declaredField.get(TTVImageUtilError.class)) == code) {
                    return declaredField.getName();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return "unknown error";
    }
}

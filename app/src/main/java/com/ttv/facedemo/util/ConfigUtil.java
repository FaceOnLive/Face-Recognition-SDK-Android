package com.ttv.facedemo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import com.ttv.facedemo.R;
import com.ttv.facedemo.common.Constants;
import com.ttv.face.enums.DetectFaceOrientPriority;

public class ConfigUtil {

    private static final float RECOMMEND_RECOGNIZE_THRESHOLD = 0.80f;

    private static final float RECOMMEND_SHELTER_THRESHOLD = 0.50f;

    private static final float RECOMMEND_EYE_OPEN_THRESHOLD = 0.50f;

    private static final float RECOMMEND_MOUTH_CLOSE_THRESHOLD = 0.50f;

    private static final float RECOMMEND_WEAR_GLASSES_THRESHOLD = 0.50f;

    private static final float RECOMMEND_RGB_LIVENESS_THRESHOLD = 0.50f;

    private static final float RECOMMEND_IR_LIVENESS_THRESHOLD = 0.70f;

    private static final float RECOMMEND_LIVENESS_FQ_THRESHOLD = 0.65f;

    private static final int RECOMMEND_RGB_LIVENESS_FACE_SIZE_THRESHOLD = 80;

    private static final int RECOMMEND_IR_LIVENESS_FACE_SIZE_THRESHOLD = 90;

    public static final float IMAGE_QUALITY_NO_MASK_RECOGNIZE_THRESHOLD = 0.49f;

    public static final float IMAGE_QUALITY_NO_MASK_REGISTER_THRESHOLD = 0.63f;

    public static final float IMAGE_QUALITY_MASK_RECOGNIZE_THRESHOLD = 0.29f;


    private static final int RECOMMEND_FACE_SIZE_LIMIT = 160;

    private static final int RECOMMEND_FACE_MOVE_LIMIT = 20;

    private static final int DEFAULT_MAX_DETECT_FACE_NUM = 1;

    private static final int DEFAULT_SCALE = 16;

    private static final String DEFAULT_PREVIEW_SIZE = "1280x720";


    private static String getString(Context context, @StringRes int keyRes, String defaultValue) {
        if (context == null) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(keyRes);
        return sharedPreferences.getString(key, defaultValue);
    }

    private static boolean getBoolean(Context context, @StringRes int keyRes, boolean defaultValue) {
        if (context == null) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(keyRes);
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    private static int getInt(Context context, @StringRes int keyRes, int defaultValue) {
        if (context == null) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(keyRes);
        return sharedPreferences.getInt(key, defaultValue);
    }

    private static float getFloat(Context context, @StringRes int keyRes, float defaultValue) {
        if (context == null) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(keyRes);
        return sharedPreferences.getFloat(key, defaultValue);
    }

    private static boolean commitInt(Context context, @StringRes int keyRes, int newValue) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.edit()
                .putInt(context.getString(keyRes), newValue)
                .commit();
    }

    private static boolean commitString(Context context, @StringRes int keyRes, String newValue) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.edit()
                .putString(context.getString(keyRes), newValue)
                .commit();
    }

    public static boolean setTrackedFaceCount(Context context, int trackedFaceCount) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.edit()
                .putInt(context.getString(R.string.preference_track_face_count), trackedFaceCount)
                .commit();
    }

    public static int getTrackedFaceCount(Context context) {
        return getInt(context, R.string.preference_track_face_count, 0);
    }

    public static DetectFaceOrientPriority getFtOrient(Context context) {
        if (context == null) {
            return DetectFaceOrientPriority.TTV_OP_ALL_OUT;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return DetectFaceOrientPriority.valueOf(sharedPreferences.getString(context.getString(R.string.preference_choose_detect_degree), DetectFaceOrientPriority.TTV_OP_ALL_OUT.name()));
    }

    public static boolean isKeepMaxFace(Context context) {
//        return getBoolean(context, R.string.preference_recognize_keep_max_face, false);
        return true;
    }

    public static boolean isRecognizeAreaLimited(Context context) {
        return getBoolean(context, R.string.preference_recognize_limit_recognize_area, false);
    }

    public static int getRecognizeMaxDetectFaceNum(Context context) {
        try {
            return Integer.parseInt(getString(context, R.string.preference_recognize_max_detect_num, String.valueOf(DEFAULT_MAX_DETECT_FACE_NUM)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return DEFAULT_MAX_DETECT_FACE_NUM;
    }

    public static int getRecognizeScale(Context context) {
        try {
            return Integer.parseInt(getString(context, R.string.preference_recognize_scale_value, String.valueOf(DEFAULT_SCALE)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return DEFAULT_SCALE;
    }

    public static int getDualCameraHorizontalOffset(Context context) {
        return getInt(context, R.string.preference_dual_camera_offset_horizontal, 0);
    }

    public static int getDualCameraVerticalOffset(Context context) {
        return getInt(context, R.string.preference_dual_camera_offset_vertical, 0);
    }

    public static float getRecognizeThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_recognize_threshold, String.valueOf(RECOMMEND_RECOGNIZE_THRESHOLD)));
    }

    public static float getRecognizeShelterThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_shelter_threshold, String.valueOf(RECOMMEND_SHELTER_THRESHOLD)));
    }

    public static float getRecognizeEyeOpenThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_eye_open_threshold, String.valueOf(RECOMMEND_EYE_OPEN_THRESHOLD)));
    }

    public static float getRecognizeMouthCloseThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_mouth_close_threshold, String.valueOf(RECOMMEND_MOUTH_CLOSE_THRESHOLD)));
    }

    public static float getRecognizeWearGlassesThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_wear_glasses_threshold, String.valueOf(RECOMMEND_WEAR_GLASSES_THRESHOLD)));
    }

    public static float getRgbLivenessThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_rgb_liveness_threshold, String.valueOf(RECOMMEND_RGB_LIVENESS_THRESHOLD)));
    }

    public static float getIrLivenessThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_ir_liveness_threshold, String.valueOf(RECOMMEND_IR_LIVENESS_THRESHOLD)));
    }

    public static float getLivenessFqThreshold(Context context){
        return Float.parseFloat(getString(context, R.string.preference_liveness_fq_threshold, String.valueOf(RECOMMEND_LIVENESS_FQ_THRESHOLD)));
    }

    public static int getRgbLivenessFaceSizeThreshold(Context context) {
        return Integer.parseInt(getString(context, R.string.preference_rgb_liveness_face_size_threshold, String.valueOf(RECOMMEND_RGB_LIVENESS_FACE_SIZE_THRESHOLD)));
    }

    public static int getIrLivenessFaceSizeThreshold(Context context) {
        return Integer.parseInt(getString(context, R.string.preference_ir_liveness_face_size_threshold, String.valueOf(RECOMMEND_IR_LIVENESS_FACE_SIZE_THRESHOLD)));
    }

    public static float getImageQualityNoMaskRecognizeThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_image_quality_no_mask_recognize_threshold,
                String.valueOf(IMAGE_QUALITY_NO_MASK_RECOGNIZE_THRESHOLD)));
    }

    public static float getImageQualityNoMaskRegisterThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_image_quality_no_mask_register_threshold,
                String.valueOf(IMAGE_QUALITY_NO_MASK_REGISTER_THRESHOLD)));
    }

    public static float getImageQualityMaskRecognizeThreshold(Context context) {
        return Float.parseFloat(getString(context, R.string.preference_image_quality_mask_recognize_threshold,
                String.valueOf(IMAGE_QUALITY_MASK_RECOGNIZE_THRESHOLD)));
    }

    public static int getFaceSizeLimit(Context context) {
        return Integer.parseInt(getString(context, R.string.preference_recognize_face_size_limit, String.valueOf(RECOMMEND_FACE_SIZE_LIMIT)));
    }

    public static int getFaceMoveLimit(Context context) {
        return Integer.parseInt(getString(context, R.string.preference_recognize_move_pixel_limit, String.valueOf(RECOMMEND_FACE_MOVE_LIMIT)));
    }

    public static String getLivenessDetectType(Context context) {
        return getString(context, R.string.preference_liveness_detect_type, context.getString(R.string.value_liveness_type_rgb));
    }


    public static boolean isEnableImageQualityDetect(Context context) {
        return getBoolean(context, R.string.preference_enable_image_quality_detect, true);
    }

    public static boolean isEnableFaceSizeLimit(Context context) {
        return getBoolean(context, R.string.preference_enable_face_size_limit, false);
    }

    public static boolean isEnableFaceMoveLimit(Context context) {
        return getBoolean(context, R.string.preference_enable_face_move_limit, false);
    }

    public static boolean isSwitchCamera(Context context) {
        return getBoolean(context, R.string.preference_switch_camera, true);
    }

    public static String getPreviewSize(Context context) {
        return getString(context, R.string.preference_dual_camera_preview_size, DEFAULT_PREVIEW_SIZE);
    }

    public static String getRgbCameraAdditionalRotation(Context context) {
        return getString(context, R.string.preference_rgb_camera_rotation, "0");
    }

    public static String getIrCameraAdditionalRotation(Context context) {
        return getString(context, R.string.preference_ir_camera_rotation, "0");
    }

    public static boolean isDrawRgbRectHorizontalMirror(Context context) {
        return getBoolean(context, R.string.preference_draw_rgb_rect_horizontal_mirror, false);
    }

    public static boolean isDrawIrRectHorizontalMirror(Context context) {
        return getBoolean(context, R.string.preference_draw_ir_rect_horizontal_mirror, false);
    }

    public static boolean isDrawRgbRectVerticalMirror(Context context) {
        return getBoolean(context, R.string.preference_draw_rgb_rect_vertical_mirror, false);
    }

    public static boolean isDrawIrRectVerticalMirror(Context context) {
        return getBoolean(context, R.string.preference_draw_ir_rect_vertical_mirror, false);
    }

    public static boolean isDrawRgbPreviewHorizontalMirror(Context context) {
        return getBoolean(context, R.string.preference_rgb_preview_horizontal_mirror, false);
    }

    public static boolean isDrawIrPreviewHorizontalMirror(Context context) {
        return getBoolean(context, R.string.preference_ir_preview_horizontal_mirror, false);
    }

}

package com.ttv.facedemo.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.preference.EditTextPreference;



public class AdjustableIntegerPreference extends EditTextPreference {

    public AdjustableIntegerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AdjustableIntegerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AdjustableIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdjustableIntegerPreference(Context context) {
        super(context);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index,0);
    }

}

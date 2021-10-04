package com.ttv.facedemo.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RestrictTo;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import com.ttv.facedemo.R;

public class ThresholdPreferenceDialogFragmentCompat extends EditTextPreferenceDialogFragmentCompat implements View.OnClickListener {

    private EditText mDialogEditText;
    public static ThresholdPreferenceDialogFragmentCompat newInstance(String key) {
        final ThresholdPreferenceDialogFragmentCompat
                fragment = new ThresholdPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View prefThresholdSetting) {
        super.onBindDialogView(prefThresholdSetting);
        ImageView mIvIncrease = prefThresholdSetting.findViewById(R.id.iv_increase);
        ImageView mIvDecrease = prefThresholdSetting.findViewById(R.id.iv_decrease);
        mIvIncrease.setOnClickListener(this);
        mIvDecrease.setOnClickListener(this);

        mDialogEditText = prefThresholdSetting.findViewById(android.R.id.edit);
    }

    @RestrictTo(LIBRARY)
    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mDialogEditText.getText().toString();
            if (!isContentValid(value)) {
                Toast.makeText(getContext(), R.string.threshold_value_illegal, Toast.LENGTH_SHORT).show();
                return;
            }
            final ThresholdPreference preference = (ThresholdPreference) getPreference();
            if (preference.callChangeListener(value)) {
                preference.setText(value);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_increase:
                increase();
                break;
            case R.id.iv_decrease:
                decrease();
                break;
            default:
                break;
        }
    }

    private boolean isContentValid(float number) {
        return (number >= 0 && number <= 1.0);
    }

    private boolean isContentValid(String number) {
        float threshold = 0;
        try {
            threshold = Float.parseFloat(number);
            return isContentValid(threshold);
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private void increase() {
        if (TextUtils.isEmpty(mDialogEditText.getText())) {
            return;
        }
        float threshold = Float.parseFloat(mDialogEditText.getText().toString());
        if (threshold <= 0.99f) {
            threshold += 0.011f;
            mDialogEditText.setText(String.format("%.2f",threshold));
        }
    }

    private void decrease() {
        if (TextUtils.isEmpty(mDialogEditText.getText())) {
            return;
        }
        float threshold = Float.parseFloat(mDialogEditText.getText().toString());
        if (threshold >= 1) {
            threshold = 1;
        }
        if (threshold >= 0.01f) {
            threshold -= 0.009f;
            mDialogEditText.setText(String.format("%.2f",threshold));
        }
    }
}

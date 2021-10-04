package com.ttv.facedemo.preference;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RestrictTo;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import com.ttv.facedemo.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

public class ThresholdLivePreferenceDialogFragmentCompat extends EditTextPreferenceDialogFragmentCompat implements View.OnClickListener {

    private EditText mDialogEditText;

    public static ThresholdLivePreferenceDialogFragmentCompat newInstance(String key) {
        final ThresholdLivePreferenceDialogFragmentCompat fragment = new ThresholdLivePreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View prefThresholdSetting) {
        super.onBindDialogView(prefThresholdSetting);
        prefThresholdSetting.findViewById(R.id.iv_live_increase).setOnClickListener(this);
        prefThresholdSetting.findViewById(R.id.iv_live_decrease).setOnClickListener(this);
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
                Toast.makeText(getContext(), R.string.interger_value_illegal, Toast.LENGTH_SHORT).show();
                return;
            }
            final ThresholdLivePreference preference = (ThresholdLivePreference) getPreference();
            if (preference.callChangeListener(value)) {
                preference.setText(value);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_live_increase:
                increaseLive();
                break;
            case R.id.iv_live_decrease:
                decreaseLive();
                break;
            default:
                break;
        }
    }

    private boolean isContentValid(int number) {
        return (number >= 0 && number <= 4096);
    }

    private boolean isContentValid(String number) {
        int threshold;
        try {
            threshold = Integer.parseInt(number);
            return isContentValid(threshold);
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private void increaseLive() {
        if (TextUtils.isEmpty(mDialogEditText.getText())) {
            return;
        }
        int threshold = Integer.parseInt(mDialogEditText.getText().toString());
        if (threshold <= 4095) {
            threshold += 1;
            mDialogEditText.setText(String.format("%d",threshold));
        }
    }

    private void decreaseLive() {
        if (TextUtils.isEmpty(mDialogEditText.getText())) {
            return;
        }
        int threshold = Integer.parseInt(mDialogEditText.getText().toString());
        if (threshold >= 4096) {
            threshold = 4096;
        }
        if (threshold >= 1) {
            threshold -= 1;
            mDialogEditText.setText(String.format("%d",threshold));
        }
    }
}

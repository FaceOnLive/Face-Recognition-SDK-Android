package com.ttv.facedemo.preference;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RestrictTo;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import com.ttv.facedemo.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

public class IntegerPreferenceDialogFragmentCompat extends EditTextPreferenceDialogFragmentCompat implements View.OnClickListener {

    private EditText dialogEditText;

    public static IntegerPreferenceDialogFragmentCompat newInstance(String key) {
        final IntegerPreferenceDialogFragmentCompat
                fragment = new IntegerPreferenceDialogFragmentCompat();
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
        dialogEditText = prefThresholdSetting.findViewById(android.R.id.edit);
    }

    @RestrictTo(LIBRARY)
    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = dialogEditText.getText().toString();
            if (!isContentValid(value)) {
                Toast.makeText(getContext(), R.string.interger_value_illegal, Toast.LENGTH_SHORT).show();
                return;
            }
            final AdjustableIntegerPreference integerPreference = (AdjustableIntegerPreference) getPreference();
            if (integerPreference.callChangeListener(value)) {
                integerPreference.setText(value);
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

    private boolean isContentValid(int number) {
        return (number >= 0);
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

    private void increase() {
        if (TextUtils.isEmpty(dialogEditText.getText())) {
            return;
        }
        int threshold = Integer.parseInt(dialogEditText.getText().toString());
        threshold++;
        dialogEditText.setText(String.format("%d", threshold));

    }

    private void decrease() {
        if (TextUtils.isEmpty(dialogEditText.getText())) {
            return;
        }
        int threshold = Integer.parseInt(dialogEditText.getText().toString());
        threshold--;
        dialogEditText.setText(String.format("%d", threshold));
    }

}

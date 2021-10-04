package com.ttv.facedemo.preference;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class RecognizeSettingsPreferenceFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_FRAGMENT_TAG =
            "androidx.preference.RecognizeSettingsPreferenceFragment.DIALOG";

    private static HashMap<Class<? extends DialogPreference>, Class<? extends PreferenceDialogFragmentCompat>> classMap = new HashMap<>();

    static {
        classMap.put(ThresholdPreference.class, ThresholdPreferenceDialogFragmentCompat.class);
        classMap.put(ThresholdLivePreference.class, ThresholdLivePreferenceDialogFragmentCompat.class);
        classMap.put(AdjustableIntegerPreference.class, IntegerPreferenceDialogFragmentCompat.class);
        classMap.put(ListPreference.class, ListPreferenceDialogFragmentCompat.class);
        classMap.put(MultiSelectListPreference.class, MultiSelectListPreferenceDialogFragmentCompat.class);
        classMap.put(EditTextPreference.class, EditTextPreferenceDialogFragmentCompat.class);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        boolean handled = false;
        if (getCallbackFragment() instanceof OnPreferenceDisplayDialogCallback) {
            handled = ((OnPreferenceDisplayDialogCallback) getCallbackFragment())
                    .onPreferenceDisplayDialog(this, preference);
        }
        if (!handled && getActivity() instanceof OnPreferenceDisplayDialogCallback) {
            handled = ((OnPreferenceDisplayDialogCallback) getActivity())
                    .onPreferenceDisplayDialog(this, preference);
        }

        if (handled) {
            return;
        }

        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        DialogFragment f = null;

        Class fragmentCompatClazz = classMap.get(preference.getClass());
        if (fragmentCompatClazz != null) {
            try {
                Method newInstanceMethod = fragmentCompatClazz.getMethod("newInstance", String.class);
                f = (DialogFragment) newInstanceMethod.invoke(fragmentCompatClazz, preference.getKey());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (f == null) {
            super.onDisplayPreferenceDialog(preference);
        }else {
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
    }
}

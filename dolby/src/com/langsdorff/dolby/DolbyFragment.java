/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.android.settingslib.widget.MainSwitchPreference;

public class DolbyFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_CUSTOM_BANDS = "eq_custom_bands";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager manager = getPreferenceManager();
        manager.setStorageDeviceProtected();
        manager.setSharedPreferencesName(DolbyController.PREFS_NAME);
        setPreferencesFromResource(R.xml.dolby_settings, rootKey);

        DolbyController controller = DolbyController.get(requireContext());
        MainSwitchPreference sw = findPreference(DolbyController.KEY_ENABLED);
        sw.setChecked(controller.isEnabled());
        sw.setOnPreferenceChangeListener((preference, newValue) -> {
            controller.setEnabled((Boolean) newValue);
            return true;
        });

        PreferenceCategory bands = findPreference(KEY_CUSTOM_BANDS);
        String[] labels = getResources().getStringArray(R.array.eq_band_labels);
        for (int band = 0; band < SessionEffects.NUM_BANDS; band++) {
            SeekBarPreference pref = new SeekBarPreference(requireContext());
            pref.setKey(SessionEffects.KEY_EQ_BAND + band);
            pref.setTitle(labels[band]);
            pref.setMin(SessionEffects.BAND_MIN);
            pref.setMax(SessionEffects.BAND_MAX);
            pref.setDefaultValue(0);
            pref.setShowSeekBarValue(true);
            pref.setUpdatesContinuously(false);
            pref.setIconSpaceReserved(false);
            bands.addPreference(pref);
        }
        updateBands();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == null) {
            return;
        }
        if (key.equals(DolbyController.KEY_PROFILE)) {
            DolbyController.get(requireContext()).apply();
        } else if (key.startsWith("eq_")) {
            updateBands();
            SessionEffects.get(requireContext()).applyAll();
        }
    }

    private void updateBands() {
        ListPreference preset = findPreference(SessionEffects.KEY_EQ_PRESET);
        PreferenceCategory bands = findPreference(KEY_CUSTOM_BANDS);
        bands.setVisible(String.valueOf(SessionEffects.PRESET_CUSTOM).equals(preset.getValue()));
    }
}

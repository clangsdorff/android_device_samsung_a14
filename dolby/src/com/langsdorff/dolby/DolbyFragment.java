/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.android.settingslib.widget.MainSwitchPreference;

public class DolbyFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dolby_settings, rootKey);
        DolbyController controller = DolbyController.get(requireContext());
        MainSwitchPreference sw = findPreference(DolbyController.KEY_ENABLED);
        sw.setChecked(controller.isEnabled());
        sw.setOnPreferenceChangeListener((preference, newValue) -> {
            controller.setEnabled((Boolean) newValue);
            return true;
        });
    }
}

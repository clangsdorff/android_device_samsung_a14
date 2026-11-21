/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.content.Context;
import android.content.SharedPreferences;

public final class DolbyController {
    static final String KEY_ENABLED = "dolby_enable";
    static final String KEY_PROFILE = "dolby_profile";
    static final String PREFS_NAME = "dolby";

    private static DolbyController sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;

    private DolbyController(Context context) {
        mContext = context;
        mPrefs = prefs(context);
    }

    static SharedPreferences prefs(Context context) {
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized DolbyController get(Context context) {
        if (sInstance == null) {
            sInstance = new DolbyController(context.getApplicationContext());
        }
        return sInstance;
    }

    public boolean isEnabled() {
        return mPrefs.getBoolean(KEY_ENABLED, false);
    }

    public int getProfile() {
        return Integer.parseInt(mPrefs.getString(KEY_PROFILE, "0"));
    }

    public synchronized void setEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        apply();
    }

    public void apply() {
        SessionEffects.get(mContext).applyAll();
    }
}

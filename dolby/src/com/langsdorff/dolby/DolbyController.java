/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.util.Log;

import java.util.UUID;

public final class DolbyController {
    private static final String TAG = "DolbySettings";
    static final String KEY_ENABLED = "dolby_enable";
    static final String KEY_PROFILE = "dolby_profile";
    static final String PREFS_NAME = "dolby";

    // profile ids from vendor etc/dolby/dax-default.xml, stock boots with none selected (-1)
    private static final int PARAM_PROFILE = 0;

    // dap_proxy in vendor audio_effects.xml
    private static final UUID DAP_UUID = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");

    private static DolbyController sInstance;

    private final SharedPreferences mPrefs;
    private AudioEffect mEffect;

    private DolbyController(Context context) {
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

    public synchronized void apply() {
        boolean enabled = isEnabled();
        if (mEffect == null) {
            if (!enabled) {
                return;
            }
            try {
                mEffect = new AudioEffect(AudioEffect.EFFECT_TYPE_NULL, DAP_UUID,
                        Integer.MAX_VALUE, 0);
            } catch (RuntimeException e) {
                Log.e(TAG, "failed to create dap effect", e);
                return;
            }
        }
        int ret = mEffect.setEnabled(enabled);
        if (ret != AudioEffect.SUCCESS) {
            Log.e(TAG, "setEnabled(" + enabled + ") failed: " + ret);
        }
        if (enabled) {
            ret = mEffect.setParameter(PARAM_PROFILE, getProfile());
            if (ret != AudioEffect.SUCCESS) {
                Log.e(TAG, "setProfile(" + getProfile() + ") failed: " + ret);
            }
        } else {
            mEffect.release();
            mEffect = null;
        }
    }
}

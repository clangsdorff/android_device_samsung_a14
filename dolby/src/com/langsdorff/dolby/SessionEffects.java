/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SoundAlive and SoundBooster are suspended by AudioFlinger on the output mix,
 * so like stock they are attached to every media playback session. Dolby goes
 * there too: on the output mix it sees the signal after the volume is applied
 * and its volume leveler lifts it back up.
 */
public final class SessionEffects extends AudioManager.AudioPlaybackCallback {
    private static final String TAG = "DolbySettings";

    static final String KEY_EQ_ENABLED = "eq_enable";
    static final String KEY_EQ_PRESET = "eq_preset";
    static final String KEY_EQ_BAND = "eq_band_";

    static final int PRESET_CUSTOM = 5;
    static final int NUM_BANDS = 9;
    static final int BAND_MIN = -12;
    static final int BAND_MAX = 12;

    // proxy uuids from vendor audio_effects.xml
    private static final UUID SOUNDALIVE_UUID = UUID.fromString("05227ea0-50bb-11e3-ac69-0002a5d5c51b");
    private static final UUID SOUNDBOOSTER_UUID = UUID.fromString("50de45f0-5d4c-11e5-a837-0800200c9a66");
    private static final UUID DAP_UUID = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");

    // stock libcorefx DolbyHandleAction, sent by stock audioflinger for apps without a dolby callback
    private static final int DAP_PARAM_PROFILE = 0;
    private static final int DAP_PARAM_REVERT_VOLUME_LEVELER = 12;
    private static final int DAP_PARAM_PROFILE_ENABLE = 19;
    private static final int DAP_PARAM_CONTENT_PROC_ENABLE = 21;
    private static final int DAP_PARAM_CONTENT_TYPE = 26;

    private static final int PARAM_BAND_LEVEL = 2;

    // stock SoundAlive_V app tables, index is the preset id
    private static final short[][] BANDS_SPEAKER = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 5, 4, 0, -1, -1, -1, -1},
        {0, 0, 2, 1, -1, -2, -2, -3, -5},
        {0, 0, 5, 4, 1, -1, 1, 4, 5},
        {0, 0, -1, 0, 1, 4, 2, 0, 2},
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, -2, 0, 1, 0, 1, 4, 5},
    };
    private static final short[][] BANDS_HEADPHONE = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {5, 5, 4, 1, 0, 0, 0, 0, 0},
        {1, 3, 4, 2, 0, 0, 0, -3, -5},
        {2, 3, 3, 0, -1, 1, 3, 5, 5},
        {-3, -1, 1, 1, 2, 5, 4, 2, 2},
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {-1, -1, 0, 0, 1, 1, 3, 5, 5},
    };

    private static final AudioAttributes MEDIA_ATTRIBUTES = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build();

    private static SessionEffects sInstance;

    private final DolbyController mDolby;
    private final AudioManager mAudioManager;
    private final SharedPreferences mPrefs;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final SparseArray<Attached> mSessions = new SparseArray<>();

    private static final class Attached {
        AudioEffect soundAlive;
        AudioEffect soundBooster;
        AudioEffect dolby;
    }

    private SessionEffects(Context context) {
        mDolby = DolbyController.get(context);
        mAudioManager = context.getSystemService(AudioManager.class);
        mPrefs = DolbyController.prefs(context);
    }

    public static synchronized SessionEffects get(Context context) {
        if (sInstance == null) {
            sInstance = new SessionEffects(context.getApplicationContext());
        }
        return sInstance;
    }

    public void start() {
        mAudioManager.registerAudioPlaybackCallback(this, mHandler);
        mAudioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] devices) {
                applyAll();
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
                applyAll();
            }
        }, mHandler);
        onPlaybackConfigChanged(mAudioManager.getActivePlaybackConfigurations());
    }

    public boolean isEqEnabled() {
        return mPrefs.getBoolean(KEY_EQ_ENABLED, false);
    }

    public int getPreset() {
        return Integer.parseInt(mPrefs.getString(KEY_EQ_PRESET, "0"));
    }

    public int getCustomBand(int band) {
        return mPrefs.getInt(KEY_EQ_BAND + band, 0);
    }

    @Override
    public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
        Set<Integer> live = new HashSet<>();
        for (AudioPlaybackConfiguration config : configs) {
            int usage = config.getAudioAttributes().getUsage();
            if (usage != AudioAttributes.USAGE_MEDIA && usage != AudioAttributes.USAGE_GAME
                    && usage != AudioAttributes.USAGE_UNKNOWN) {
                continue;
            }
            int session = config.getSessionId();
            if (session > 0) {
                live.add(session);
            }
        }
        for (int i = mSessions.size() - 1; i >= 0; i--) {
            int session = mSessions.keyAt(i);
            if (!live.contains(session)) {
                release(mSessions.valueAt(i));
                mSessions.removeAt(i);
            }
        }
        for (int session : live) {
            if (mSessions.get(session) == null) {
                Attached attached = new Attached();
                attached.soundBooster = create(SOUNDBOOSTER_UUID, session);
                if (attached.soundBooster != null) {
                    attached.soundBooster.setEnabled(true);
                }
                mSessions.put(session, attached);
                apply(session, attached);
            }
        }
    }

    public void applyAll() {
        mHandler.post(() -> {
            for (int i = 0; i < mSessions.size(); i++) {
                apply(mSessions.keyAt(i), mSessions.valueAt(i));
            }
        });
    }

    private void apply(int session, Attached attached) {
        applyDolby(session, attached);
        applyEqualizer(session, attached);
    }

    private void applyDolby(int session, Attached attached) {
        if (!mDolby.isEnabled()) {
            if (attached.dolby != null) {
                attached.dolby.release();
                attached.dolby = null;
            }
            return;
        }
        if (attached.dolby == null) {
            attached.dolby = create(DAP_UUID, session);
            if (attached.dolby == null) {
                return;
            }
        }
        try {
            attached.dolby.setEnabled(true);
            attached.dolby.setParameter(DAP_PARAM_PROFILE_ENABLE, 1);
            attached.dolby.setParameter(DAP_PARAM_PROFILE, mDolby.getProfile());
            attached.dolby.setParameter(DAP_PARAM_REVERT_VOLUME_LEVELER, 0);
            attached.dolby.setParameter(DAP_PARAM_CONTENT_PROC_ENABLE, 1);
            attached.dolby.setParameter(DAP_PARAM_CONTENT_TYPE, 0);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to apply dolby on session " + session, e);
        }
    }

    private void applyEqualizer(int session, Attached attached) {
        if (!isEqEnabled()) {
            if (attached.soundAlive != null) {
                attached.soundAlive.release();
                attached.soundAlive = null;
            }
            return;
        }
        if (attached.soundAlive == null) {
            attached.soundAlive = create(SOUNDALIVE_UUID, session);
            if (attached.soundAlive == null) {
                return;
            }
        }
        short[] levels = currentLevels();
        try {
            attached.soundAlive.setEnabled(true);
            for (int band = 0; band < NUM_BANDS; band++) {
                attached.soundAlive.setParameter(new int[] {PARAM_BAND_LEVEL, band},
                        new short[] {levels[band]});
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to apply equalizer on session " + session, e);
        }
    }

    private short[] currentLevels() {
        int preset = getPreset();
        boolean speaker = isSpeaker();
        short[] levels = new short[NUM_BANDS];
        for (int band = 0; band < NUM_BANDS; band++) {
            if (preset == PRESET_CUSTOM) {
                levels[band] = (short) getCustomBand(band);
            } else {
                levels[band] = (speaker ? BANDS_SPEAKER : BANDS_HEADPHONE)[preset][band];
            }
            // stock drops the two lowest bands on the speaker
            if (speaker && band < 2) {
                levels[band] = 0;
            }
        }
        return levels;
    }

    private boolean isSpeaker() {
        List<AudioDeviceAttributes> devices = mAudioManager.getDevicesForAttributes(MEDIA_ATTRIBUTES);
        for (AudioDeviceAttributes device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE) {
                return true;
            }
        }
        return devices.isEmpty();
    }

    private static AudioEffect create(UUID uuid, int session) {
        try {
            return new AudioEffect(AudioEffect.EFFECT_TYPE_NULL, uuid, Integer.MAX_VALUE, session);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to create " + uuid + " on session " + session, e);
            return null;
        }
    }

    private static void release(Attached attached) {
        if (attached.soundAlive != null) {
            attached.soundAlive.release();
        }
        if (attached.soundBooster != null) {
            attached.soundBooster.release();
        }
        if (attached.dolby != null) {
            attached.dolby.release();
        }
    }
}

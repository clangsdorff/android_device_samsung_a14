/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stock audioflinger attaches Dolby, SoundAlive and SoundBooster to media sessions; here they are
 * attached per playback session from the playback callback. Effects on the output mix are
 * suspended by AudioFlinger as soon as any session effect is enabled, so the session is the only
 * place they keep running.
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
    private static final UUID DAP_UUID = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");

    // stock libcorefx DolbyHandleAction, sent by stock audioflinger for apps without a dolby callback
    private static final int DAP_PARAM_PROFILE = 0;
    private static final int DAP_PARAM_REVERT_VOLUME_LEVELER = 12;
    private static final int DAP_PARAM_PROFILE_ENABLE = 19;
    private static final int DAP_PARAM_CONTENT_PROC_ENABLE = 21;
    private static final int DAP_PARAM_CONTENT_TYPE = 26;

    private static final int PARAM_BAND_LEVEL = 2;

    // AudioManager.VOLUME_CHANGED_ACTION and EXTRA_VOLUME_STREAM_TYPE are @hide
    private static final String ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

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

    private final Context mContext;
    private final DolbyController mDolby;
    private final AudioManager mAudioManager;
    private final SharedPreferences mPrefs;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final SparseArray<Attached> mSessions = new SparseArray<>();

    private static final class Attached {
        AudioEffect dolby;
        int dolbyProfile = -1;
        AudioEffect soundAlive;
        short[] soundAliveLevels;
        SpeakerDynamics speaker;
    }

    private SessionEffects(Context context) {
        mContext = context;
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
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC) {
                    int row = speakerRow();
                    for (int i = 0; i < mSessions.size(); i++) {
                        SpeakerDynamics speaker = mSessions.valueAt(i).speaker;
                        if (speaker != null) {
                            speaker.rampTo(row);
                        }
                    }
                }
            }
        }, new IntentFilter(ACTION_VOLUME_CHANGED), null, mHandler);
        mAudioManager.addOnModeChangedListener(mContext.getMainExecutor(), mode -> applyAll());
        mAudioManager.addOnCommunicationDeviceChangedListener(
                mContext.getMainExecutor(), device -> applyAll());
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
            if (!live.contains(mSessions.keyAt(i))) {
                release(mSessions.valueAt(i));
                mSessions.removeAt(i);
            }
        }
        for (int session : live) {
            if (mSessions.get(session) == null) {
                Attached attached = new Attached();
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
        boolean inCall = isInCallOrCommunication();
        int device = getOutputDeviceType(inCall);
        boolean earpiece = device == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
        boolean speaker = device == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
        applyDolby(session, attached, mDolby.isEnabled() && !inCall && !earpiece);
        applySoundAlive(session, attached, isEqEnabled() && !inCall, speaker || earpiece);
        applySpeaker(session, attached, speaker && !inCall);
    }

    // parameters are only sent when they change, re-sending them on a playing stream pops
    private void applyDolby(int session, Attached attached, boolean enabled) {
        if (!enabled) {
            attached.dolby = release(attached.dolby);
            attached.dolbyProfile = -1;
            return;
        }
        if (attached.dolby == null) {
            attached.dolby = create(DAP_UUID, session);
            if (attached.dolby == null) {
                return;
            }
        }
        int profile = mDolby.getProfile();
        if (profile == attached.dolbyProfile) {
            return;
        }
        try {
            attached.dolby.setEnabled(true);
            attached.dolby.setParameter(DAP_PARAM_PROFILE_ENABLE, 1);
            attached.dolby.setParameter(DAP_PARAM_PROFILE, profile);
            attached.dolby.setParameter(DAP_PARAM_REVERT_VOLUME_LEVELER, 0);
            attached.dolby.setParameter(DAP_PARAM_CONTENT_PROC_ENABLE, 1);
            attached.dolby.setParameter(DAP_PARAM_CONTENT_TYPE, 0);
            attached.dolbyProfile = profile;
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to apply dolby on session " + session, e);
        }
    }

    private void applySoundAlive(int session, Attached attached, boolean enabled, boolean builtIn) {
        if (!enabled) {
            attached.soundAlive = release(attached.soundAlive);
            attached.soundAliveLevels = null;
            return;
        }
        if (attached.soundAlive == null) {
            attached.soundAlive = create(SOUNDALIVE_UUID, session);
            if (attached.soundAlive == null) {
                return;
            }
        }
        short[] levels = currentLevels(builtIn);
        if (Arrays.equals(levels, attached.soundAliveLevels)) {
            return;
        }
        try {
            attached.soundAlive.setEnabled(true);
            for (int band = 0; band < NUM_BANDS; band++) {
                attached.soundAlive.setParameter(new int[] {PARAM_BAND_LEVEL, band},
                        new short[] {levels[band]});
            }
            attached.soundAliveLevels = levels;
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to apply equalizer on session " + session, e);
        }
    }

    private void applySpeaker(int session, Attached attached, boolean enabled) {
        if (!enabled) {
            if (attached.speaker != null) {
                attached.speaker.release();
                attached.speaker = null;
            }
            return;
        }
        if (attached.speaker == null) {
            attached.speaker = SpeakerDynamics.attach(session, speakerRow(), mHandler);
        } else {
            attached.speaker.rampTo(speakerRow());
        }
    }

    private int speakerRow() {
        return SpeakerDynamics.rowForVolume(
                mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    }

    private short[] currentLevels(boolean builtIn) {
        int preset = getPreset();
        short[] levels = new short[NUM_BANDS];
        for (int band = 0; band < NUM_BANDS; band++) {
            if (preset == PRESET_CUSTOM) {
                levels[band] = (short) getCustomBand(band);
            } else {
                levels[band] = (builtIn ? BANDS_SPEAKER : BANDS_HEADPHONE)[preset][band];
            }
            // stock drops the two lowest bands on built-in speaker and earpiece
            if (builtIn && band < 2) {
                levels[band] = 0;
            }
        }
        return levels;
    }

    private boolean isInCallOrCommunication() {
        int mode = mAudioManager.getMode();
        return mode == AudioManager.MODE_IN_CALL
                || mode == AudioManager.MODE_IN_COMMUNICATION
                || mode == AudioManager.MODE_CALL_SCREENING;
    }

    private int getOutputDeviceType(boolean inCall) {
        if (inCall) {
            AudioDeviceInfo comm = mAudioManager.getCommunicationDevice();
            if (comm != null) {
                return comm.getType();
            }
        }
        List<AudioDeviceAttributes> devices = mAudioManager.getDevicesForAttributes(MEDIA_ATTRIBUTES);
        for (AudioDeviceAttributes device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                return AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
            }
            if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE) {
                return AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
            }
        }
        return devices.isEmpty() ? AudioDeviceInfo.TYPE_BUILTIN_SPEAKER : devices.get(0).getType();
    }

    private static AudioEffect create(UUID uuid, int session) {
        try {
            return new AudioEffect(AudioEffect.EFFECT_TYPE_NULL, uuid, Integer.MAX_VALUE, session);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to create " + uuid + " on session " + session, e);
            return null;
        }
    }

    private static AudioEffect release(AudioEffect effect) {
        if (effect != null) {
            effect.release();
        }
        return null;
    }

    private static void release(Attached attached) {
        release(attached.dolby);
        release(attached.soundAlive);
        if (attached.speaker != null) {
            attached.speaker.release();
        }
    }
}

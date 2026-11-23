/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.media.audiofx.DynamicsProcessing;
import android.os.Handler;
import android.util.Log;

/**
 * Open-source stand-in for the stock Samsung SoundBooster on the built-in speaker, built on
 * DynamicsProcessing from the stock /vendor/etc/SoundBoosterParam.txt tables: a high-pass filter
 * and a ten band peaking EQ (AH and AA rows), then an eleven band boost-and-ceiling compressor
 * (AM rows). Like stock, the row follows the media volume, and changes are ramped.
 */
final class SpeakerDynamics {
    private static final String TAG = "DolbySettings";

    private static final int SAMPLE_RATE = 48000;
    // 1024 point frames so the 100-150 Hz high-pass lands on more than one fft bin
    private static final float FRAME_DURATION_MS = 20.0f;

    // stock SoundBooster_Music volume index (0..15) to param row A..E
    private static final int[] ROW_FOR_VOLUME = {0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 3, 4, 4};
    private static final int ROWS = 5;

    // AHA..AHE, 4th order butterworth (Filter::GetHPFcoeffs)
    private static final float[] HPF_HZ = {100, 100, 120, 140, 150};

    // AAA..AAE peaking bands (Filter::BQcoeff_compute type 1): centre Hz, bandwidth Hz, gain dB per row
    private static final float[] PEQ_HZ = {220, 704, 3700, 6000, 1500, 11200, 3200, 1300, 4400, 350};
    private static final float[] PEQ_BW = {250, 200, 500, 3000, 400, 3000, 400, 400, 1200, 200};
    private static final float[][] PEQ_GAIN = {
        {5, -7, -8, 8, 4, -1, -3, -3, 0, -3},
        {5, -7, -8, 8, 4, -1, -3, -3, 0, -3},
        {5, -7, -9, 7, 4, -2, -4, -3, -1, -3},
        {5, -7, -10, 5, 4, -3, -5, -3, -2, -3},
        {5, -7, -11, 4, 4, -4, -6, -3, -3, -3},
    };

    // DynamicsProcessing pre-eq is a per fft bin gain, the stock curve is sampled per band
    private static final float[] EQ_CUTOFF = {
        70, 117, 164, 211, 258, 352, 469, 600, 750, 950, 1200, 1500,
        1850, 2300, 2850, 3500, 4300, 5300, 6500, 8000, 10000, 12500, 16000, 24000,
    };

    // lib_SoundBooster_ver1100 FFT_MB: 512 point fft at 48 kHz, crossovers are the band window overlaps
    private static final float[] MBC_CUTOFF = {
        375, 656, 937, 1219, 1781, 3000, 4969, 7031, 9000, 10969, 24000,
    };
    private static final float[] MBC_ATTACK = {10, 10, 10, 10, 10, 25, 25, 25, 25, 25, 25};
    private static final float MBC_RELEASE = 150;
    // AMA..AME, per band boost and output ceiling in dB
    private static final float[][] MBC_GAIN = {
        {5, 10, 9, 1, 0, 2, 3, 2, 6, 8, 7},
        {5, 10, 9, 1, 0, 2, 3, 2, 6, 8, 7},
        {4, 9, 8, 0, 0, 2, 3, 2, 5, 6, 5},
        {3, 8, 7, 0, 0, 2, 3, 2, 4, 5, 4},
        {2, 8, 6, 0, 0, 2, 3, 2, 3, 4, 3},
    };
    private static final float[][] MBC_CEILING = {
        {-10, -8, -9, -16, -17, -8, -8, -13, -10, -7, -11},
        {-10, -8, -9, -16, -17, -8, -8, -13, -10, -7, -11},
        {-10, -8, -9, -16, -17, -8, -8, -13, -10, -7, -11},
        {-10, -8, -9, -16, -17, -8, -8, -13, -10, -7, -11},
        {-9, -7, -8, -16, -17, -8, -8, -13, -10, -7, -11},
    };

    private static final float[][] EQ_GAIN = buildEqGains();

    private static final int RAMP_STEPS = 10;
    private static final long RAMP_INTERVAL_MS = 30;

    private final DynamicsProcessing mEffect;
    private final Handler mHandler;
    private final float[] mEq = new float[EQ_CUTOFF.length];
    private final float[] mGain = new float[MBC_CUTOFF.length];
    private final float[] mCeiling = new float[MBC_CUTOFF.length];
    private int mRow = -1;
    private Runnable mRamp;

    private SpeakerDynamics(DynamicsProcessing effect, Handler handler) {
        mEffect = effect;
        mHandler = handler;
    }

    /** Attaches flat and fades in to the row, a step on a playing stream clicks. */
    static SpeakerDynamics attach(int session, int row, Handler handler) {
        DynamicsProcessing effect = null;
        try {
            effect = new DynamicsProcessing(0, session, flatConfig());
            effect.setEnabled(true);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to attach speaker dynamics on session " + session, e);
            if (effect != null) {
                effect.release();
            }
            return null;
        }
        SpeakerDynamics dynamics = new SpeakerDynamics(effect, handler);
        dynamics.rampTo(row);
        return dynamics;
    }

    static int rowForVolume(int index, int max) {
        int scaled = max > 0 ? Math.round(index * 15.0f / max) : 15;
        return ROW_FOR_VOLUME[Math.max(0, Math.min(15, scaled))];
    }

    void rampTo(int row) {
        if (row == mRow) {
            return;
        }
        mRow = row;
        if (mRamp != null) {
            mHandler.removeCallbacks(mRamp);
        }
        float[] eq = mEq.clone();
        float[] gain = mGain.clone();
        float[] ceiling = mCeiling.clone();
        mRamp = new Runnable() {
            private int mStep = 1;

            @Override
            public void run() {
                float t = (float) mStep / RAMP_STEPS;
                try {
                    for (int i = 0; i < mEq.length; i++) {
                        float v = lerp(eq[i], EQ_GAIN[row][i], t);
                        if (v != mEq[i]) {
                            mEq[i] = v;
                            mEffect.setPreEqBandAllChannelsTo(i, eqBand(i, v));
                        }
                    }
                    for (int i = 0; i < mGain.length; i++) {
                        float g = lerp(gain[i], MBC_GAIN[row][i], t);
                        float c = lerp(ceiling[i], MBC_CEILING[row][i], t);
                        if (g != mGain[i] || c != mCeiling[i]) {
                            mGain[i] = g;
                            mCeiling[i] = c;
                            mEffect.setMbcBandAllChannelsTo(i, mbcBand(i, g, c));
                        }
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "failed to update speaker dynamics", e);
                    mRamp = null;
                    return;
                }
                if (++mStep <= RAMP_STEPS) {
                    mHandler.postDelayed(this, RAMP_INTERVAL_MS);
                } else {
                    mRamp = null;
                }
            }
        };
        mRamp.run();
    }

    void release() {
        if (mRamp != null) {
            mHandler.removeCallbacks(mRamp);
            mRamp = null;
        }
        mEffect.release();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static DynamicsProcessing.EqBand eqBand(int band, float gain) {
        return new DynamicsProcessing.EqBand(true, EQ_CUTOFF[band], gain);
    }

    private static DynamicsProcessing.MbcBand mbcBand(int band, float gain, float ceiling) {
        // stock gain curve: boost below the knee, saturate at the ceiling over a 10 dB knee
        return new DynamicsProcessing.MbcBand(true, MBC_CUTOFF[band], MBC_ATTACK[band], MBC_RELEASE,
                10.0f, ceiling, 10.0f, -90.0f, 1.0f, gain, 0.0f);
    }

    private static DynamicsProcessing.Config flatConfig() {
        DynamicsProcessing.Eq preEq = new DynamicsProcessing.Eq(true, true, EQ_CUTOFF.length);
        for (int i = 0; i < EQ_CUTOFF.length; i++) {
            preEq.setBand(i, eqBand(i, 0));
        }
        DynamicsProcessing.Mbc mbc = new DynamicsProcessing.Mbc(true, true, MBC_CUTOFF.length);
        for (int i = 0; i < MBC_CUTOFF.length; i++) {
            mbc.setBand(i, mbcBand(i, 0, 0));
        }
        DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(
                true, true, 0, 1.0f, 60.0f, 10.0f, -3.0f, 0.0f);
        return new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
                true, EQ_CUTOFF.length,
                true, MBC_CUTOFF.length,
                false, 0,
                true)
                .setPreferredFrameDuration(FRAME_DURATION_MS)
                .setPreEqAllChannelsTo(preEq)
                .setMbcAllChannelsTo(mbc)
                .setLimiterAllChannelsTo(limiter)
                .build();
    }

    private static float[][] buildEqGains() {
        float[][] gains = new float[ROWS][EQ_CUTOFF.length];
        for (int row = 0; row < ROWS; row++) {
            for (int band = 0; band < EQ_CUTOFF.length; band++) {
                double f = band == 0 ? SAMPLE_RATE / 1024.0
                        : Math.sqrt(EQ_CUTOFF[band - 1] * EQ_CUTOFF[band]);
                double db = highPassDb(f, HPF_HZ[row]);
                for (int i = 0; i < PEQ_HZ.length; i++) {
                    db += peakingDb(f, PEQ_HZ[i], PEQ_BW[i], PEQ_GAIN[row][i]);
                }
                gains[row][band] = (float) db;
            }
        }
        return gains;
    }

    private static double highPassDb(double f, double cutoff) {
        return -10.0 * Math.log10(1.0 + Math.pow(cutoff / f, 8));
    }

    private static double peakingDb(double f, double centre, double bandwidth, double gainDb) {
        double w0 = 2.0 * Math.PI * centre / SAMPLE_RATE;
        double half = Math.PI * bandwidth / SAMPLE_RATE;
        double octaves = Math.log((w0 + half) / (w0 - half)) / Math.log(2.0);
        double alpha = Math.sin(w0) * Math.sinh(Math.log(2.0) / 2.0 * octaves * w0 / Math.sin(w0));
        double a = Math.pow(10.0, gainDb / 40.0);
        double cos = Math.cos(w0);
        double w = 2.0 * Math.PI * f / SAMPLE_RATE;
        double num = magnitude(1.0 + alpha * a, -2.0 * cos, 1.0 - alpha * a, w);
        double den = magnitude(1.0 + alpha / a, -2.0 * cos, 1.0 - alpha / a, w);
        return 20.0 * Math.log10(num / den);
    }

    private static double magnitude(double c0, double c1, double c2, double w) {
        double re = c0 + c1 * Math.cos(w) + c2 * Math.cos(2.0 * w);
        double im = -c1 * Math.sin(w) - c2 * Math.sin(2.0 * w);
        return Math.hypot(re, im);
    }
}

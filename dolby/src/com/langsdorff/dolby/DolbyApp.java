/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.app.Application;

public class DolbyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DolbyController.get(this).apply();
        SessionEffects.get(this).start();
    }
}

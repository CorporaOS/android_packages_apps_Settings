/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.Flags;

import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller that accesses and switches the preference status of
 * magnifying the nav bar and IME feature.
 */
public class MagnifyNavAndImePreferenceController extends TogglePreferenceController {

    private static final String TAG =
            MagnifyNavAndImePreferenceController.class.getSimpleName();
    static final String PREF_KEY = "accessibility_magnify_nav_and_ime";

    private SwitchPreference mMagnifyNavImePreference;

    public MagnifyNavAndImePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.magnifyNavAndIme() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFY_NAV_AND_IME,
                OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFY_NAV_AND_IME,
                (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}

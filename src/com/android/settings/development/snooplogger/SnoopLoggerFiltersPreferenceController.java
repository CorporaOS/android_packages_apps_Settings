/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development.snooplogger;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.SnoopLoggerFiltersUtils;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.Map;

/**
 * A {@link BasePreferenceController} used in {@link SnoopLoggerFiltersDashboard}
 */
public class SnoopLoggerFiltersPreferenceController extends BasePreferenceController {
    private static final String TAG = "SnoopLoggerFiltersPreferenceController";

    public SnoopLoggerFiltersPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final String currentValue = SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        return TextUtils.equals(
                currentValue, SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceGroup mGroup = screen.findPreference(getPreferenceKey());
        final Map<String, String> featureMap = SnoopLoggerFiltersUtils.getSnoopLoggerFilterTypes();
        mGroup.removeAll();
        final Context prefContext = mGroup.getContext();
        featureMap.keySet().stream().sorted().forEach(feature ->
                mGroup.addPreference(new SnoopLoggerFiltersPreference(prefContext, feature)));
    }
}

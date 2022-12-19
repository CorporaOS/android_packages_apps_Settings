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
import android.util.SnoopLoggerFiltersUtils;

import androidx.preference.SwitchPreference;

/**
 * Snoop Logger filter switch preference.
 */
public class SnoopLoggerFiltersPreference extends SwitchPreference {

    private final String mKey;
    private static final String TAG = "SnoopLoggerFiltersPreference";

    public SnoopLoggerFiltersPreference(Context context, String key) {
        super(context);
        mKey = key;
        setKey(key);
        setTitle(SnoopLoggerFiltersUtils.getSnoopLoggerFilterTypes().get(mKey));

        boolean isFilterEnabled;

        isFilterEnabled = SystemProperties.get(
            SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY).contains(key);
        super.setChecked(isFilterEnabled);
    }

    @Override
    public void setChecked(boolean isChecked) {
        super.setChecked(isChecked);
        SnoopLoggerFiltersUtils.setEnabled(getContext(), mKey, isChecked);
    }
}

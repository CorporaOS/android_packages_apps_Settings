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
import android.sysprop.BluetoothProperties;

import androidx.preference.SwitchPreference;

public class SnoopLoggerFiltersPreference extends SwitchPreference {

    private final String mKey;
    private static final String TAG = "SnoopLoggerFiltersPreference";

    public SnoopLoggerFiltersPreference(Context context, String key, String entry) {
        super(context);
        mKey = key;
        setKey(key);
        setTitle(entry);

        var snoopLogFilterTypesList = BluetoothProperties.snoop_log_filter_types();

        boolean isFilterEnabled = snoopLogFilterTypesList.contains(
                BluetoothProperties.snoop_log_filter_types_values.valueOf(key.toUpperCase()));

        super.setChecked(isFilterEnabled);
    }

    @Override
    public void setChecked(boolean isChecked) {
        super.setChecked(isChecked);

        var snoopLogFilterTypesList = BluetoothProperties.snoop_log_filter_types();
        var filterTypeValue =
                BluetoothProperties.snoop_log_filter_types_values.valueOf(mKey.toUpperCase());

        if (isChecked) {
            if (snoopLogFilterTypesList.contains(filterTypeValue)) {
                return;
            }
            snoopLogFilterTypesList.add(filterTypeValue);
        } else {
            if (!snoopLogFilterTypesList.contains(filterTypeValue)) {
                return;
            }
            snoopLogFilterTypesList.remove(filterTypeValue);
        }

        if (snoopLogFilterTypesList.contains("")) {
            snoopLogFilterTypesList.remove("");
        }

        BluetoothProperties.snoop_log_filter_types(snoopLogFilterTypesList);
    }
}

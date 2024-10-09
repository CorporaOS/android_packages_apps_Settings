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
package com.android.settings.applications.appinfo;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class Enable16KbAppCompatDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_COMPAT_SETTINGS_SWITCH = "app_compat_settings_switch";

    private TwoStatePreference mSwitchPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        addPreferencesFromResource(R.xml.enable_16kb_app_compat_details);
        mSwitchPref = findPreference(KEY_APP_COMPAT_SETTINGS_SWITCH);
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    protected boolean refreshUi() {
        mSwitchPref.setEnabled(true);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    //TODO: add a enum for app-compat
    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ENABLE_16KB_APP_COMPAT;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        return getSummary(context);
    }

    public static CharSequence getSummary(Context context) {
        return context.getString(R.string.enable_16k_app_compat_details);
    }
}

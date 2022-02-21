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

package com.android.settings.development;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.system.Os;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Switch to enable Pointer Authentication on capable devices.
 */
public class PointerAuthenticationPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String PAUTH_PREF = "pointer_auth";

    private static final String PAC_PROPERTY = "persist.dalvik.vm.isa.arm64.features.pac.enabled";

    public PointerAuthenticationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        String machine = Os.uname().machine;
        if (!machine.matches("^aarch.*|^arm.*"))
            return false;
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String regex = "^Features\\s*:.*\\bpaca\\b.*";
            String line;
            while ((line = br.readLine()) != null) {
                if (line.matches(regex)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return PAUTH_PREF;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(PAC_PROPERTY, isEnabled ? Boolean.toString(true) : null);
        displayPointerAuthenticationToast();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enablePAC = SystemProperties.getBoolean(
                PAC_PROPERTY, false /* default */);
        ((SwitchPreference) mPreference).setChecked(enablePAC);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        if (SystemProperties.getBoolean(PAC_PROPERTY, false))
            displayPointerAuthenticationToast();
        SystemProperties.set(PAC_PROPERTY, Boolean.toString(false));
        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    void displayPointerAuthenticationToast() {
        Toast.makeText(mContext, R.string.pointer_auth_toast, Toast.LENGTH_LONG).show();
    }
}

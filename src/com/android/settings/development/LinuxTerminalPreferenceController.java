/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

public class LinuxTerminalPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String TAG = "LinuxTerminalPrefCtrl";

    private static final String ENABLE_TERMINAL_KEY = "enable_linux_terminal";
    private static final String TERMINAL_APP_ACTION = "android.virtualization.VM_TERMINAL";

    private final PackageManager mPackageManager;
    private final ComponentName mTerminalComponent;

    public LinuxTerminalPreferenceController(Context context) {
        super(context);
        mPackageManager = mContext.getPackageManager();
        mTerminalComponent = resolveTerminalActivity();
    }

    // This may be called before displayPreference(). Avoid lazy initialization.
    @Override
    public boolean isAvailable() {
        return getTerminalComponentName() != null;
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_TERMINAL_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setEnabled(isAvailable());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean terminalEnabled = (Boolean) newValue;
        if (terminalEnabled) {
            mPackageManager.setComponentEnabledSetting(
                    getTerminalComponentName(),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            mPackageManager.setComponentEnabledSetting(
                    getTerminalComponentName(),
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    /* flags= */ 0);
        }
        ((TwoStatePreference) mPreference).setChecked(terminalEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isTerminalEnabled = mPackageManager.getComponentEnabledSetting(
                getTerminalComponentName()) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ((TwoStatePreference) mPreference).setChecked(isTerminalEnabled);
    }

    // Can be mocked for testing
    @VisibleForTesting
    ComponentName getTerminalComponentName() {
        return mTerminalComponent;
    }

    private ComponentName resolveTerminalActivity() {
        Intent intent = new Intent(TERMINAL_APP_ACTION);
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent,
                PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS);
        if (resolveInfos.size() != 1) {
            Log.w(TAG,
                    "Failed to resolve activity, action=" + TERMINAL_APP_ACTION
                    + ", resolved=" + resolveInfos);
            return null;
        }
        ActivityInfo activityInfo = resolveInfos.getFirst().activityInfo;
        return new ComponentName(activityInfo.packageName, activityInfo.name);
    }
}

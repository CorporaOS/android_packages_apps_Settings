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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class LinuxTerminalPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String ENABLE_TERMINAL_KEY = "enable_linux_terminal";

    @VisibleForTesting
    static final String TERMINAL_APP_PACKAGE = "com.android.virtualization.terminal";

    private PackageManager mPackageManager;
    private UserManager mUserManager;

    public LocalTerminalPreferenceController(Context context) {
        super(context);

        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return isPackageInstalled(TERMINAL_APP_PACKAGE);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_TERMINAL_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPackageManager = getPackageManager();

        if (isAvailable() && !isEnabled()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean terminalEnabled = (Boolean) newValue;
        if (terminalEnabled) {
            mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    /* flags= */ 0);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isTerminalEnabled = mPackageManager.getApplicationEnabledSetting(
                TERMINAL_APP_PACKAGE) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ((TwoStatePreference) mPreference).setChecked(isTerminalEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isEnabled()) {
            mPreference.setEnabled(true);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    PackageManager getPackageManager() {
        return mContext.getPackageManager();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            return mContext.getPackageManager().getPackageInfo(packageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isEnabled() {
        return mUserManager.isAdminUser();
    }
}

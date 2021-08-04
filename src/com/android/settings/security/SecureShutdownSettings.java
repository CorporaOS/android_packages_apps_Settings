/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.FooterPreference;


public class SecureShutdownSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener {

    private static final String KEY_ENABLE_SECURE_SHUTDOWN_ON_KEYGUARD  =
            "enable_secure_shutdown_on_keyguard";
    private static final String KEY_ENABLE_SECURE_SHUTDOWN_THROUGHOUT =
            "enable_secure_shutdown_throughout";
    private static final String KEY_FOOTER = "secure_shutdown_settings_screen_footer";
    private static final int CHANGE_DEVICE_SECURITY_REQUEST_CODE = 1;

    private SwitchBar mSwitchBar;
    private SwitchPreference mUseSecureShutdownOnKeyguard;
    private SwitchPreference mUseSecureShutdownThroughout;
    private FooterPreference mFooterPreference;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURE_SHUTDOWN;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        activity.setTitle(R.string.secure_shutdown_title);
        mLockPatternUtils = new LockPatternUtils(activity);

        addPreferencesFromResource(R.xml.secure_shutdown_settings);
        final PreferenceScreen root = getPreferenceScreen();
        mUseSecureShutdownOnKeyguard = root.findPreference(KEY_ENABLE_SECURE_SHUTDOWN_ON_KEYGUARD);
        mUseSecureShutdownThroughout = root.findPreference(KEY_ENABLE_SECURE_SHUTDOWN_THROUGHOUT);
        mFooterPreference = root.findPreference(KEY_FOOTER);

        mSwitchBar = activity.getSwitchBar();
        if (checkSecurityEnabled()) {
            setSecureShutdownEnabled(false);
            setSecureShutdownOnKeyguard(false);
            setSecureShutdownThroughout(false);
        }
        mSwitchBar.show();
        mSwitchBar.setChecked(isSecureShutdownEnabled(getActivity()));
        mSwitchBar.addOnSwitchChangeListener(this);
        updateDisplay();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_secure_shutdown;
    }

    private static boolean isSecureShutdownEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SECURE_SHUTDOWN_ENABLED, 0) != 0;
    }

    private void setSecureShutdownEnabled(boolean isEnabled) {
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.SECURE_SHUTDOWN_ENABLED,
                isEnabled ? 1 : 0);
        // Set the default value to match what we have defaulted to in the UI
        // as this would be the major use of the feature.
        setSecureShutdownOnKeyguard(isEnabled);

    }

    private boolean isSecureShutdownOnKeyguardEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SECURE_SHUTDOWN_ENABLED_ON_KEYGUARD, 0) != 0;
    }

    private boolean setSecureShutdownOnKeyguard(boolean isEnabled) {
        return Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.SECURE_SHUTDOWN_ENABLED_ON_KEYGUARD, isEnabled ? 1 : 0);
    }

    private boolean isSecureShutdownThroughoutEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SECURE_SHUTDOWN_ENABLED_THROUGHOUT, 0) != 0;
    }

    private boolean setSecureShutdownThroughout(boolean isEnabled) {
        return Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.SECURE_SHUTDOWN_ENABLED_THROUGHOUT, isEnabled ? 1 : 0);
    }

    private boolean setSubSettings(boolean isEnabled, String type) {
        if (type.equals(KEY_ENABLE_SECURE_SHUTDOWN_ON_KEYGUARD)) {
            return setSecureShutdownOnKeyguard(isEnabled);
        } else {
            return setSecureShutdownThroughout(isEnabled);
        }
        return false;
    }

    private boolean checkSecurityEnabled() {
        LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
        int passwordQuality = lockPatternUtils
                .getKeyguardStoredPasswordQuality(UserHandle.myUserId());
        return passwordQuality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    private void setSecureShutdown(boolean isEnabled) {
        if (isEnabled) {
            if (checkSecurityEnabled()) {
                Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                chooseLockIntent.putExtra(
                        ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                startActivityForResult(chooseLockIntent, CHANGE_DEVICE_SECURITY_REQUEST_CODE);
                return;
            }
        }
        setSecureShutdownEnabled(isEnabled);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_DEVICE_SECURITY_REQUEST_CODE) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            boolean validPassQuality = lockPatternUtils.getKeyguardStoredPasswordQuality(
                    UserHandle.myUserId())
                    != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
            setSecureShutdown(validPassQuality);
            mSwitchBar.setChecked(validPassQuality);
            updateDisplay();
        }
    }


    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setSecureShutdown(isChecked);
        updateDisplay();

    }

    private void updateDisplay() {
        if (isSecureShutdownEnabled(getActivity())) {
            mUseSecureShutdownOnKeyguard.setVisible(true);
            mUseSecureShutdownThroughout.setVisible(true);
            mUseSecureShutdownOnKeyguard.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            //If both settings are switched off, it means secure shutdown is off
                            //so switch off the master switch and update the settings.
                            if (!(boolean) newValue && !mUseSecureShutdownThroughout.isChecked()) {
                                mSwitchBar.setChecked(false);
                                setSecureShutdownEnabled(false);
                                updateDisplay();
                            }
                            return setSubSettings((boolean) newValue,
                                    KEY_ENABLE_SECURE_SHUTDOWN_ON_KEYGUARD);
                        }
                    });
            mUseSecureShutdownThroughout.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener(){
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (!(boolean) newValue && !mUseSecureShutdownOnKeyguard.isChecked()) {
                                mSwitchBar.setChecked(false);
                                setSecureShutdownEnabled(false);
                                updateDisplay();
                            }
                            return setSubSettings((boolean) newValue,
                                    KEY_ENABLE_SECURE_SHUTDOWN_THROUGHOUT);
                        }
                    });
            mUseSecureShutdownOnKeyguard
                    .setChecked(isSecureShutdownOnKeyguardEnabled(getActivity()));
            mUseSecureShutdownOnKeyguard.setTitle(R.string.secure_shutdown_on_keyguard_title);
            mUseSecureShutdownThroughout
                    .setChecked(isSecureShutdownThroughoutEnabled(getActivity()));
            mUseSecureShutdownThroughout.setTitle(R.string.secure_shutdown_throughout_title);
        } else {
            mUseSecureShutdownOnKeyguard.setVisible(false);
            mUseSecureShutdownThroughout.setVisible(false);
        }
        mFooterPreference.setSummary(R.string.secure_shutdown_description);
    }
}

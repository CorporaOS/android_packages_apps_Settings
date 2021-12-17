/*
 * Copyright 2021 The Android Open Source Project
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
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller for Bluetooth LE audio hardware offload feature
 */
public class BluetoothLeAudioHwOffloadPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String PREFERENCE_KEY =
            "bluetooth_disable_le_audio_hw_offload";

    static final String LE_AUDIO_OFFLOAD_ENABLED_PROPERTY =
            "persist.bluetooth.leaudio_offload.enabled";
    static final String LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY =
            "ro.bluetooth.leaudio_offload.supported";

    public BluetoothLeAudioHwOffloadPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isToggleEnabled = (Boolean) newValue;
        SystemProperties.set(LE_AUDIO_OFFLOAD_ENABLED_PROPERTY,
                isEnabled ? "false" : "true");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean offloadSupported =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, false);
        if (offloadSupported) {
            final boolean offloadEnabled =
                    SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_ENABLED_PROPERTY, false);
            ((SwitchPreference) mPreference).setChecked(!offloadEnabled);
        } else {
            mPreference.setEnabled(false);
            ((SwitchPreference) mPreference).setChecked(true);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        final boolean offloadSupported =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, false);
        if (offloadSupported) {
            ((SwitchPreference) mPreference).setChecked(false);
            SystemProperties.set(LE_AUDIO_OFFLOAD_ENABLED_PROPERTY, "true");
        }
    }
}

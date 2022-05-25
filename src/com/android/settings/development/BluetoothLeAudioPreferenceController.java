/*
 * Copyright 2022 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Preference controller to control Bluetooth LE audio feature
 */
public class BluetoothLeAudioPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_enable_leaudio";
    private final DevelopmentSettingsDashboardFragment mFragment;

    private static final String[] LE_AUDIO_RELATED_PROFILES_PROPERTIES = new String[]{
        "bluetooth.profile.bap.unicast.client.enabled",
        "bluetooth.profile.csip.set_coordinator.enabled",
        "bluetooth.profile.hap.client.enabled",
        "bluetooth.profile.mcp.server.enabled",
        "bluetooth.profile.ccp.server.enabled",
        "bluetooth.profile.vcp.controller.enabled",
    };

    private static final String LE_AUDIO_DYNAMIC_SWITCH_PROPERTY =
            "ro.bluetooth.leaudio_switcher.supported";

    private static final String PROPERTY_FILE = "/data/local.prop";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting
    boolean mChanged = false;

    public BluetoothLeAudioPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        BluetoothRebootDialog.show(mFragment);
        mChanged = true;
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        if (mBluetoothAdapter == null) {
            return;
        }

        final boolean leAudioSwitchSupported =
                SystemProperties.getBoolean(LE_AUDIO_DYNAMIC_SWITCH_PROPERTY, false);
        final boolean leAudioEnabled =
                (mBluetoothAdapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED);

        if (!leAudioSwitchSupported) {
            mPreference.setEnabled(false);
        }
        ((SwitchPreference) mPreference).setChecked(leAudioEnabled);
    }

    /**
     * Called when the RebootDialog confirm is clicked.
     */
    public void onRebootDialogConfirmed() {
        if (!mChanged || mBluetoothAdapter == null) {
            return;
        }

        final boolean leAudioEnabled =
                (mBluetoothAdapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED);
        switchLeAudioProfileStatus(!leAudioEnabled);
    }

    /**
     * Called when the RebootDialog cancel is clicked.
     */
    public void onRebootDialogCanceled() {
        mChanged = false;
    }

    @VisibleForTesting
    void switchLeAudioProfileStatus(boolean enable) {
        try {
            File file = new File(PROPERTY_FILE);
            BufferedWriter writer;
            writer = new BufferedWriter(new FileWriter(
                PROPERTY_FILE));
            if (file.exists() && file.length() != 0) {
                BufferedReader reader;
                reader = new BufferedReader(new FileReader(PROPERTY_FILE));
                String line = reader.readLine();
                while (line != null) {
                    if (!line.startsWith("bluetooth.profile.")) {
                        writer.write(line);
                        writer.newLine();
                    } else {
                        boolean  found = false;
                        for (int i = 0; i < LE_AUDIO_RELATED_PROFILES_PROPERTIES.length; i++) {
                            if (line.startsWith(LE_AUDIO_RELATED_PROFILES_PROPERTIES[i])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                    line = reader.readLine();
                }
                reader.close();
            } else {
                file.createNewFile();
            }

            for (int i = 0; i < LE_AUDIO_RELATED_PROFILES_PROPERTIES.length; i++) {
                writer.write(LE_AUDIO_RELATED_PROFILES_PROPERTIES[i] + "=" + enable);
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.development.BluetoothLeAudioDeviceDetailsPreferenceController
        .LE_AUDIO_DEVICE_DETAIL_ENABLED_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioDeviceDetailsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private BluetoothLeAudioDeviceDetailsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new BluetoothLeAudioDeviceDetailsPreferenceController(
                        mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldTurnOnLeAudioDeviceDetailSetting() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean isEnabled = SystemProperties.getBoolean(
                LE_AUDIO_DEVICE_DETAIL_ENABLED_PROPERTY, false /* default */);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldTurnOffLeAudioDeviceDetailSetting() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean isEnabled = SystemProperties.getBoolean(
                LE_AUDIO_DEVICE_DETAIL_ENABLED_PROPERTY, false /* default */);

        assertThat(isEnabled).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        SystemProperties.set(LE_AUDIO_DEVICE_DETAIL_ENABLED_PROPERTY, Boolean.toString(true));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        SystemProperties.set(LE_AUDIO_DEVICE_DETAIL_ENABLED_PROPERTY, Boolean.toString(false));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }
}

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

package com.android.settings.development;

import static com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
        .LE_AUDIO_OFFLOAD_ENABLED_PROPERTY;
import static com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
        .LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioHwOffloadPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private BluetoothLeAudioHwOffloadPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.application;
        mController = new BluetoothLeAudioHwOffloadPreferenceController(context);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_offloadNotSupported_preferenceShouldBeDisabled() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(false));
        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldNotBeChecked() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(LE_AUDIO_OFFLOAD_ENABLED_PROPERTY, Boolean.toString(true));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldBeChecked() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(LE_AUDIO_OFFLOAD_ENABLED_PROPERTY, Boolean.toString(false));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }
}

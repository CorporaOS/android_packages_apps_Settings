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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.SnoopLoggerFiltersUtils;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothSnoopLogFilterProfileMapPreferenceControllerTest {

    @Spy
    private Context mSpyContext = RuntimeEnvironment.application;
    @Spy
    private Resources mSpyResources = RuntimeEnvironment.application.getResources();
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private BluetoothSnoopLogFilterProfileMapPreferenceController mController;

    private CharSequence[] mListValues;
    private CharSequence[] mListEntries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(mSpyResources).when(mSpyContext).getResources();
        // Get XML values without mock
        // Setup test list preference using XML values
        mPreference = new ListPreference(mSpyContext);
        mPreference.setEntries(R.array.bt_hci_snoop_log_profile_filter_entries);
        mPreference.setEntryValues(R.array.bt_hci_snoop_log_profile_filter_values);
        // Init the actual controller
        mController = new BluetoothSnoopLogFilterProfileMapPreferenceController(mSpyContext);
        // Construct preference in the controller via a mocked preference screen object
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mListValues = mPreference.getEntryValues();
        mListEntries = mPreference.getEntries();
        SystemProperties.set(SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED);
        SystemProperties.set(SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES);
    }

    @Test
    public void verifyResourceSizeAndRange() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        // Verify normal list entries and default preference entries have the same size
        assertThat(mListEntries.length).isEqualTo(mListValues.length);
        // Update the preference
        mController.updateState(mPreference);
        // Verify default preference value, entry and summary
        final int defaultIndex = mController.getDefaultModeIndex();
        assertThat(mPreference.getValue()).isEqualTo(mListValues[defaultIndex]);
        assertThat(mPreference.getEntry()).isEqualTo(mListEntries[defaultIndex]);
        assertThat(mPreference.getSummary()).isEqualTo(mListEntries[defaultIndex]);
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogFullFilterMap() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        mController.onPreferenceChange(null,
                mListValues[BluetoothSnoopLogFilterProfileMapPreferenceController.BTSNOOP_LOG_PROFILE_FILTER_MODE_FULL_FILTER_INDEX]);
        final String mode = SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY);
        // "fullfilter" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo("fullfilter");
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogHeaderFilterMap() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        mController.onPreferenceChange(null,
                mListValues[BluetoothSnoopLogFilterProfileMapPreferenceController.BTSNOOP_LOG_PROFILE_FILTER_MODE_HEADER_INDEX]);
        final String mode = SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY);
        // "header" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo("header");
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogMagicFilterMap() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        mController.onPreferenceChange(null,
                mListValues[BluetoothSnoopLogFilterProfileMapPreferenceController.BTSNOOP_LOG_PROFILE_FILTER_MODE_MAGIC_INDEX]);
        final String mode = SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY);
        // "magic" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo("magic");
    }

    @Test
    public void onPreferenceChanged_turnOffBluetoothSnoopLogFilterMap() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        mController.onPreferenceChange(null,
                mListValues[BluetoothSnoopLogFilterProfileMapPreferenceController.BTSNOOP_LOG_PROFILE_FILTER_MODE_DISABLED_INDEX]);
        final String mode = SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY);
        // "disabled" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo("disabled");
    }

    @Test
    public void updateState_preferenceShouldBeSetToRightValue() {
        assertThat(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
        for (int i = 0; i < mListValues.length; ++i) {
            SystemProperties.set(SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY,
                    mListValues[i].toString());
            mController.updateState(mPreference);
            assertThat(mPreference.getValue()).isEqualTo(mListValues[i].toString());
            assertThat(mPreference.getSummary()).isEqualTo(mListEntries[i].toString());
        }
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getValue()).isEqualTo(
                mListValues[mController.getDefaultModeIndex()]
                        .toString());
        assertThat(mPreference.getSummary()).isEqualTo(
                mListEntries[mController.getDefaultModeIndex()]
                        .toString());
    }
}

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
import android.sysprop.BluetoothProperties;

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

    @Spy private Context mSpyContext = RuntimeEnvironment.application;
    @Spy private Resources mSpyResources = RuntimeEnvironment.application.getResources();
    private ListPreference mPreference;
    @Mock private PreferenceScreen mPreferenceScreen;
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
        BluetoothProperties.snoop_log_mode(BluetoothProperties.snoop_log_mode_values.FILTERED);
        var filterTypesList = BluetoothProperties.snoop_log_filter_types();
        filterTypesList.clear();
        filterTypesList.add(BluetoothProperties.snoop_log_filter_types_values.PROFILESFILTERED);
        BluetoothProperties.snoop_log_filter_types(filterTypesList);
    }

    @Test
    public void verifyResourceSizeAndRange() {
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
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
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfileMapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_FULL_FILTER_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_map()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
        // "fullfilter" is hard-coded between Settings and system/bt
        assertThat(mode)
                .isEqualTo(BluetoothProperties.snoop_log_filter_profile_map_values.FULLFILTER);
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogHeaderFilterMap() {
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfileMapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_HEADER_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_map()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
        // "header" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo(BluetoothProperties.snoop_log_filter_profile_map_values.HEADER);
    }

    @Test
    public void onPreferenceChanged_turnOnBluetoothSnoopLogMagicFilterMap() {
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfileMapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_MAGIC_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_map()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
        // "magic" is hard-coded between Settings and system/bt
        assertThat(mode).isEqualTo(BluetoothProperties.snoop_log_filter_profile_map_values.MAGIC);
    }

    @Test
    public void onPreferenceChanged_turnOffBluetoothSnoopLogFilterMap() {
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
        mController.onPreferenceChange(
                null,
                mListValues[
                        BluetoothSnoopLogFilterProfileMapPreferenceController
                                .BTSNOOP_LOG_PROFILE_FILTER_MODE_DISABLED_INDEX]);
        var mode =
                BluetoothProperties.snoop_log_filter_profile_map()
                        .orElse(BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
        // "disabled" is hard-coded between Settings and system/bt
        assertThat(mode)
                .isEqualTo(BluetoothProperties.snoop_log_filter_profile_map_values.DISABLED);
    }

    @Test
    public void updateState_preferenceShouldBeSetToRightValue() {
        assertThat(
                BluetoothSnoopLogFilterProfileMapPreferenceController.isProfilesFilteringEnabled());
        for (int i = 0; i < mListValues.length; ++i) {
            BluetoothProperties.snoop_log_filter_profile_map(
                    BluetoothProperties.snoop_log_filter_profile_map_values.valueOf(
                            mListValues[i].toString().toUpperCase()));
            mController.updateState(mPreference);
            assertThat(mPreference.getValue()).isEqualTo(mListValues[i].toString());
            assertThat(mPreference.getSummary()).isEqualTo(mListEntries[i].toString());
        }
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getValue())
                .isEqualTo(mListValues[mController.getDefaultModeIndex()].toString());
        assertThat(mPreference.getSummary())
                .isEqualTo(mListEntries[mController.getDefaultModeIndex()].toString());
    }
}

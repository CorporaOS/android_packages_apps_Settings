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

package com.android.settings.development.snooplogger;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.util.SnoopLoggerFiltersUtils;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SnoopLoggerFiltersPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    private Context mContext;
    private SnoopLoggerFiltersPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SnoopLoggerFiltersPreferenceController(mContext, "test_key");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mCategory);
        when(mCategory.getContext()).thenReturn(mContext);
        mController.displayPreference(mScreen);
    }

    @Test
    public void getAvailability_debug_available() {
        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_user_unavailable() {
        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_DISABLED);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void onStart_shouldRefreshFeatureFlags() {
        mController.displayPreference(mScreen);

        verify(mCategory, atLeastOnce()).addPreference(any(SnoopLoggerFiltersPreference.class));
    }
}

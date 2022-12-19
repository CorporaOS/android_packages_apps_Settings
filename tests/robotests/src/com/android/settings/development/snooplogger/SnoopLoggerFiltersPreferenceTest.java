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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SnoopLoggerFiltersPreferenceTest {

    private static final String KEY = "profilesfiltered";
    private static final String ENTRY = "Profiles Filtered";

    private Context mContext;
    private SnoopLoggerFiltersPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SnoopLoggerFiltersPreference(mContext, KEY, ENTRY);
    }

    @Test
    public void constructor_shouldSetTitle() {
        assertThat(mPreference.getTitle()).isEqualTo(ENTRY);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_shouldSetChecked() {
        mPreference.setChecked(true);
        assertThat(mPreference.isChecked());
    }
}

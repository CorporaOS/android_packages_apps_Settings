/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FriendlyDevOptionPreferenceControllerTest {

    private static final String PLACEHOLDER_KEY = "placeholder_option";

    @Mock
    private Context mContext;

    @Mock
    private ContentResolver mContentResolver;

    @Mock
    private Preference mPreference;

    private FriendlyDevOptionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new FriendlyDevOptionPreferenceController(mContext);
    }

    @Test
    public void testGetPreferenceKey_ReturnsCorrectKey() {
        assertEquals(PLACEHOLDER_KEY, mController.getPreferenceKey());
    }

    @Test
    public void testOnPreferenceChange_EnableOption() {
        mController.onPreferenceChange(mPreference, true /* new value */);
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                PLACEHOLDER_KEY, -1 /* default */);
        assertThat(mode).isEqualTo(1);
    }

    @Test
    public void testOnPreferenceChange_DisableOption() {

        mController.onPreferenceChange(mPreference, false /* new value */);
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                PLACEHOLDER_KEY, -1 /* default */);

        assertThat(mode).isEqualTo(0);
    }
}

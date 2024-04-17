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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MAGNIFY_NAV_AND_IME)
public class MagnifyNavAndImePreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String KEY_MAGNIFY_NAV_AND_IME =
            Settings.Secure.ACCESSIBILITY_MAGNIFY_NAV_AND_IME;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SwitchPreference mSwitchPreference = new SwitchPreference(mContext);
    private final MagnifyNavAndImePreferenceController mController =
            new MagnifyNavAndImePreferenceController(mContext,
                    MagnifyNavAndImePreferenceController.PREF_KEY);

    @Before
    public void setUp() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference.setKey(MagnifyNavAndImePreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void isChecked_magnifyNavAndImeSettingDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, OFF);
        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_magnifyNavAndImeSettingEnabled_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, ON);
        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_magnifyNavAndImeDisabled_shouldEnableMagnifyNavAndIme() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, OFF);
        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_magnifyNavAndImeEnabled_shouldDisableMagnifyNavAndIme() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, ON);
        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }
}

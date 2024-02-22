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

package com.android.settings.deviceinfo.batteryinfo;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryHealthPreferenceControllerTest {
    private BatteryHealthPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new BatteryHealthPreferenceController(mContext,
                "battery_info_health");
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_healthGood_returnGood() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_good));
    }

    @Test
    public void getSummary_healthOverheat_returnOverheat() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_OVERHEAT);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_overheat));
    }

    @Test
    public void getSummary_healthDead_returnDead() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_DEAD);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_dead));
    }

    @Test
    public void getSummary_healthOverVoltage_returnOverVoltage() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_over_voltage));
    }

    @Test
    public void getSummary_healthUnspecifiedFailure_returnUnspecifiedFailure() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_unspecified_failure));
    }

    @Test
    public void getSummary_healthCold_returnCold() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_COLD);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_cold));
    }

    @Test
    public void getSummary_healthUnknown_returnUnknown() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.battery_health_unknown));
    }
}

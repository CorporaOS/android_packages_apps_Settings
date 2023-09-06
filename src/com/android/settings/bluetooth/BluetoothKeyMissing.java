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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * BluetoothKeyMissing is a receiver for a Bluetooth key missing broadcast. It
 * checks if the Bluetooth Settings is currently visible and brings up the informational dialog.
 * Otherwise it starts the BluetoothKeyMissingService which
 * starts a notification in the status bar that can be clicked to bring up the same dialog.
 */
public final class BluetoothKeyMissing extends BroadcastReceiver {
    private static final String TAG = "BluetoothKeyMissing";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        final LocalBluetoothManager mBluetoothManager = Utils.getLocalBtManager(context);
        if (TextUtils.equals(action, BluetoothDevice.ACTION_KEY_MISSING)) {
            PowerManager powerManager = context.getSystemService(PowerManager.class);
            boolean shouldShowDialog = LocalBluetoothPreferences.shouldShowDialogInForeground(
                        context, device);

            Log.d(TAG, "Receive KEY_MISSING for device=" + device + " name=" + device.getName());

            if (powerManager.isInteractive() && shouldShowDialog) {
                // Since the screen is on and the BT-related activity is in the foreground,
                // just open the dialog
                // convert broadcast intent into activity intent (same action string)
                Intent pairingIntent = BluetoothKeyMissingService.getKeyMissingDialogIntent(
                        context, intent, BluetoothDevice.EXTRA_PAIRING_INITIATOR_FOREGROUND);

                context.startActivityAsUser(pairingIntent, UserHandle.CURRENT);
            } else {
                // Put up a notification that leads to the dialog
                intent.setClass(context, BluetoothKeyMissingService.class);
                intent.setAction(BluetoothDevice.ACTION_KEY_MISSING);
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            }
        }
    }
}

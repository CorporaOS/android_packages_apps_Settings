/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * BluetoothKeyMissingDialog asks the user to enter a PIN / Passkey / simple confirmation
 * for pairing with a remote Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothKeyMissingDialog extends FragmentActivity {
    public static final String FRAGMENT_TAG = "bluetooth.key_missing.fragment";

    private BluetoothDevice mDevice = null;
    private boolean mReceiverRegistered = false;
    private static final String TAG = "BluetoothKeyMissingDialog";

    /**
     * Dismiss the dialog if the bond state changes to bonding or none for mDevice
      */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice == device
                        && (bondState == BluetoothDevice.BOND_BONDING
                            || bondState == BluetoothDevice.BOND_NONE)) {
                    dismiss();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        Intent intent = getIntent();

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (mDevice == null) {
            // Error handler for the case that dialog is started from adb command.
            finish();
            return;
        }

        DialogFragment newFragment = BluetoothKeyMissingDialogFragment.newInstance(mDevice);
        newFragment.show(getSupportFragmentManager(), FRAGMENT_TAG);

        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if pairing is started.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mReceiverRegistered = true;
    }

    void doPositiveClick() {
        Log.i("BluetoothKeyMissingDialog", "Positive click! removing bond");
        mDevice.removeBond();
        dismiss();
    }

    void doNegativeClick() {
        Log.i("BluetoothKeyMissingDialog", "Negative click! dismissing dialog");
        dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverRegistered) {
            mReceiverRegistered = false;
            unregisterReceiver(mReceiver);
        }
    }

    @VisibleForTesting
    void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }
}

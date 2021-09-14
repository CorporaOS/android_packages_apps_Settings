/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.bluetooth.oob;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;

public class BluetoothQrCodeActivity extends BluetoothQrCodeBaseActivity {
    private static final String TAG = "BtQrCodeActivity";

    @Override
    public int getMetricsCategory() {
        // TODO(optedoblivion): Figure out how to add to SettingsEnums
        return 1;//SettingsEnums.SETTINGS_BLUETOOTH_QR_SCANNER;
    }

    @Override
    protected void handleIntent(Intent intent) {
        Log.i(TAG, "handleIntent(" + intent + ")");
        String action = intent != null ? intent.getAction() : null;
        if (action == null) {
//            finish();
//            return;
        }

        showQrCodeScannerFragment();
    }

    private void showQrCodeScannerFragment() {
        BluetoothQrCodeScannerFragment fragment =
                (BluetoothQrCodeScannerFragment) mFragmentManager.findFragmentByTag("BTQRSCANNER");

        if (fragment == null) {
            fragment = new BluetoothQrCodeScannerFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment, "BTQRSCANNER");
        fragmentTransaction.commit();
    }

}

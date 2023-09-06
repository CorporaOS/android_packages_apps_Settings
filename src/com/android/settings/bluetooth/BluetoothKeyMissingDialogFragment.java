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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**

 */
public class BluetoothKeyMissingDialogFragment extends InstrumentedDialogFragment {
    private Context mContext;
    private BluetoothDevice mDevice;
    private AlertDialog mAlertDialog;

    static BluetoothKeyMissingDialogFragment newInstance(BluetoothDevice device) {
        BluetoothKeyMissingDialogFragment f = new BluetoothKeyMissingDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevice = getArguments().getParcelable(BluetoothDevice.EXTRA_DEVICE);
        mContext = getActivity();
        setShowsDialog(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Resources res = getResources();
        String name = mDevice != null ? mDevice.getAlias()
                                      : res.getString(android.R.string.unknownName);

        mAlertDialog = new AlertDialog.Builder(mContext)
            .setIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setTitle(res.getString(R.string.bluetooth_notif_key_missing_title))
            .setMessage(res.getString(R.string.bluetooth_notif_key_missing_message_long, name))
            .setPositiveButton(res.getString(
                        R.string.bluetooth_unpair_dialog_forget_confirm_button),
                (dialog, which) ->((BluetoothKeyMissingDialog) getActivity()).doPositiveClick())
            .setNegativeButton(res.getString(R.string.bluetooth_notif_key_missing_dismiss),
                (dialog, which) ->((BluetoothKeyMissingDialog) getActivity()).doNegativeClick())
            .create();

        /* Seems like other Bluetooth dialog don't need this to have rounded corners, but this one
         * would always come with sharp corners without this style addition */
        mAlertDialog.getWindow()
                .setBackgroundDrawableResource(R.drawable.sim_progress_dialog_rounded_bg);

        return mAlertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_KEY_MISSING;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        dismiss();
        getActivity().finish();
    }
}

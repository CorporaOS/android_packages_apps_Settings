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

package com.android.settings.development;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 *  Dialog when user interacts 16K pages developer option
 */
public class Enable16kPagesWarningDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "Enable16KDialog";

    /**
     * Used to display warning dialog
     */
    public static void show(Fragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final Enable16kPagesWarningDialog dialog = new Enable16kPagesWarningDialog();
            dialog.setTargetFragment(host, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ENABLE_16K_PAGES;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_enable_16k_pages_title)
                .setMessage(R.string.confirm_enable_16k_pages_text)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .setNegativeButton(android.R.string.cancel, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int buttonId) {
        final SixteenKbPagesDialogHost host = (SixteenKbPagesDialogHost) getTargetFragment();
        if (host == null) {
            return;
        }
        if (buttonId == DialogInterface.BUTTON_POSITIVE) {
            host.onEnable16kPagesConfirmed();
        } else {
            host.onEnable16kPagesDismissed();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        final SixteenKbPagesDialogHost host = (SixteenKbPagesDialogHost) getTargetFragment();
        if (host == null) {
            return;
        }
        host.onEnable16kPagesDismissed();
    }
}

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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Dialog when user interacts 16K pages developer option */
public class Enable16kPagesWarningDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "Enable16KDialog";
    private static final String DIALOG_BUNDLE_KEY = "SHOW_16K_DIALOG";
    private static Enable16kPagesPreferenceController sController;

    /** This method is used to show warning dialog to apply 16K update and reboot */
    public static void show(
            Fragment host, Enable16kPagesPreferenceController controller, boolean enable16k) {
        sController = controller;
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final Enable16kPagesWarningDialog dialog = new Enable16kPagesWarningDialog();
            Bundle bundle = new Bundle();
            bundle.putBoolean(DIALOG_BUNDLE_KEY, enable16k);
            dialog.setArguments(bundle);
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
        final Bundle bundle = getArguments();
        boolean is16kDialog = bundle.getBoolean(DIALOG_BUNDLE_KEY);
        return new AlertDialog.Builder(getActivity())
                .setTitle(
                        is16kDialog
                                ? R.string.confirm_enable_16k_pages_title
                                : R.string.confirm_enable_4k_pages_title)
                .setMessage(
                        is16kDialog
                                ? R.string.confirm_enable_16k_pages_text
                                : R.string.confirm_enable_4k_pages_text)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .setNegativeButton(android.R.string.cancel, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int buttonId) {
        if (buttonId == DialogInterface.BUTTON_POSITIVE) {
            sController.on16kPagesDialogConfirmed();
        } else {
            sController.on16kPagesDialogDismissed();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        sController.on16kPagesDialogDismissed();
    }
}

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

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.SystemUpdateManager;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *  Controller for 16K pages developer option
 */
public class Enable16kPagesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String ENABLE_16K_PAGES = "enable_16k_pages";
    private static final String DEV_OPTION_PROPERTY = "ro.product.build.16k_page.enabled";
    public static final int ENABLE_4K_PAGE_SIZE = 0;
    public static final int ENABLE_16K_PAGE_SIZE = 1;
    private static final String OTA_16k_PATH = "/system/boot_otas/boot_ota_16k.zip";
    private static final String OTA_4k_PATH = "/system/boot_otas/boot_ota_4k.zip";
    private static final String PAYLOAD_BINARY_FILE_NAME = "payload.bin";
    private static final String PAYLOAD_PROPERTIES_FILE_NAME = "payload_properties.txt";
    private static final int OFFSET_TO_FILE_NAME = 30;

    private final DevelopmentSettingsDashboardFragment mFragment;
    private boolean mEnable16k;

    public static final String TAG = "Enable16kPages";
    public static final String REBOOT_REASON = "Rebooting to apply 16K kernel update!";
    public static final String EXPERIMENTAL_UPDATE_TITLE = "Android 16K Kernel Experimental Update";

    private AlertDialog mProgressDialog;
    private AlertDialog.Builder mBuilder;

    public Enable16kPagesPreferenceController(
            Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mProgressDialog = getProgressDialog().create();
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean(DEV_OPTION_PROPERTY, false);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_16K_PAGES;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mEnable16k = (Boolean) newValue;
        Enable16kPagesWarningDialog.show(mFragment, mEnable16k);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int optionValue =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_16K_PAGES,
                        ENABLE_4K_PAGE_SIZE /* default */);

        ((SwitchPreference) mPreference).setChecked(optionValue == ENABLE_16K_PAGE_SIZE);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        // TODO(295035851) : Revert kernel when dev option turned off
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_16K_PAGES,
                ENABLE_4K_PAGE_SIZE);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    /**
     *  Called when user confirms to reboot with 16K pages
     */
    public void on16kDialogConfirmed() {
        // Show progress bar
        mProgressDialog.show();

        // Apply update in background
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    SystemUpdateManager manager =
                            (SystemUpdateManager)
                                    mContext.getSystemService(Context.SYSTEM_UPDATE_SERVICE);
                    Bundle data = manager.retrieveSystemUpdateInfo();
                    int status = data.getInt(SystemUpdateManager.KEY_STATUS);
                    if (status == SystemUpdateManager.STATUS_UNKNOWN
                            || status == SystemUpdateManager.STATUS_IDLE) {
                        installUpdate(mEnable16k);
                    }
                });
    }

    /**
     *  Called when user dismisses to reboot with 16K pages
     */
    public void on16kDialogDismissed() {}

    private AlertDialog.Builder getProgressDialog() {
        if (mBuilder == null) {
            mBuilder = new AlertDialog.Builder(mFragment.getActivity());
            mBuilder.setTitle(R.string.progress_16k_ota_title);

            final ProgressBar progressBar = new ProgressBar(mFragment.getActivity());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(params);
            mBuilder.setView(progressBar);
        }
        return mBuilder;
    }

    void installUpdate(boolean optionEnabled) {
        String updateFilePath = optionEnabled ? OTA_16k_PATH : OTA_4k_PATH;
        try {
            File updateFile = new File(updateFilePath);
            parsePayloadMetadata(updateFile);

            ThreadUtils.postOnMainThread(
                    () ->
                            Settings.Global.putInt(
                                    mContext.getContentResolver(),
                                    Settings.Global.ENABLE_16K_PAGES,
                                    optionEnabled ? ENABLE_16K_PAGE_SIZE : ENABLE_4K_PAGE_SIZE));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parsePayloadMetadata(File updateFile) throws IOException {
        boolean payloadFound = false;
        long payloadOffset = 0;
        long payloadSize = 0;

        List<String> properties = new ArrayList<>();
        try (ZipFile zip = new ZipFile(updateFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            long offset = 0;
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String fileName = zipEntry.getName();
                long extraSize = zipEntry.getExtra() == null ? 0 : zipEntry.getExtra().length;
                offset += 30 + fileName.length() + extraSize;

                if (zipEntry.isDirectory()) {
                    continue;
                }

                long length = zipEntry.getCompressedSize();
                if (PAYLOAD_BINARY_FILE_NAME.equals(fileName)) {
                    if (zipEntry.getMethod() != ZipEntry.STORED) {
                        throw new IOException("Unknown compression method.");
                    }
                    payloadFound = true;
                    payloadOffset = offset;
                    payloadSize = length;
                } else if (PAYLOAD_PROPERTIES_FILE_NAME.equals(fileName)) {
                    InputStream inputStream = zip.getInputStream(zipEntry);
                    if (inputStream != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = br.readLine()) != null) {
                            properties.add(line);
                        }
                    }
                }
                offset += length;
            }
        }

        if (!payloadFound) {
            throw new IOException("Failed to find payload in zip: " + updateFile.getAbsolutePath());
        }

        applyPayload(updateFile, payloadOffset, payloadSize, properties);
    }

    private void applyPayload(File updateFile, long payloadOffset, long payloadSize,
            List<String> properties) {
        String[] header = properties.stream().toArray(String[]::new);
        try {
            UpdateEngine updateEngine = getUpdateEngine();
            updateEngine.applyPayload(Paths.get(updateFile.getAbsolutePath()).toUri().toString(),
                    payloadOffset, payloadSize, header);
        } catch (Exception e) {
            Log.e(TAG, "Failed to install update.", e);
        }
    }

    private UpdateEngine getUpdateEngine() {
        UpdateEngine updateEngine = new UpdateEngine();
        updateEngine.bind(new OtaUpdateCallback(updateEngine));
        return updateEngine;
    }

    void displayToast(String message) {
        ThreadUtils.postOnMainThread(
                () -> Toast.makeText(mFragment.getActivity(), message, Toast.LENGTH_SHORT).show());
    }

    class OtaUpdateCallback extends UpdateEngineCallback {
        UpdateEngine mUpdateEngine;

        OtaUpdateCallback(UpdateEngine engine) {
            mUpdateEngine = engine;
        }

        @Override
        public void onStatusUpdate(int status, float percent) {}

        public PersistableBundle getUpdateInfo() {
            PersistableBundle infoBundle = new PersistableBundle();
            infoBundle.putInt(
                    SystemUpdateManager.KEY_STATUS, SystemUpdateManager.STATUS_WAITING_REBOOT);
            infoBundle.putBoolean(SystemUpdateManager.KEY_IS_SECURITY_UPDATE, false);
            infoBundle.putString(SystemUpdateManager.KEY_TITLE, EXPERIMENTAL_UPDATE_TITLE);
            return infoBundle;
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            mUpdateEngine.unbind();
            // Hide progress bar
            ThreadUtils.postOnMainThread(
                    () -> {
                        mProgressDialog.hide();
                    });

            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                Log.i(TAG, "applyPayload successful");
                // Publish system update info
                SystemUpdateManager manager =
                        (SystemUpdateManager)
                                mContext.getSystemService(Context.SYSTEM_UPDATE_SERVICE);
                manager.updateSystemUpdateInfo(getUpdateInfo());

                // Restart device to complete update
                PowerManager pm = mContext.getSystemService(PowerManager.class);
                pm.reboot(REBOOT_REASON);
            } else {
                Log.e(TAG, "applyPayload failed, error code: " + errorCode);
                displayToast(mContext.getString(R.string.toast_16k_update_failed_text));
            }
        }
    }
}

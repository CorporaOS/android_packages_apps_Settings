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

import android.annotation.Nullable;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
    private long mSize;
    private long mOffset;
    private List<String> mProperties;
    private Enumeration<? extends ZipEntry> mFileEntries;
    private ZipFile mUpdateZipFile;

    public static final String TAG = "Enable16kPages";
    @Nullable protected File mUpdateFilePath;

    private final DevelopmentSettingsDashboardFragment mFragment;

    private boolean mEnable16k;

    public Enable16kPagesPreferenceController(
            Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
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

        // TODO: Show progress bar here
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
        // TODO : Revert kernel?
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
        // Apply update in background
        ThreadUtils.postOnBackgroundThread(() -> applyUpdate(mEnable16k));
    }

    /**
     *  Called when user dismisses to reboot with 16K pages
     */
    public void on16kDialogDismissed() {}

    private void clearPayloadMetadata() throws IOException {
        mUpdateZipFile = new ZipFile(mUpdateFilePath);
        mProperties = new ArrayList<>();
        mSize = -1;
        mOffset = 0;
        mFileEntries = mUpdateZipFile.entries();
    }

    private boolean updatePayloadMetadata() throws IOException {
        long offset = 0;
        while (mFileEntries.hasMoreElements()) {
            ZipEntry entry = mFileEntries.nextElement();
            String name = entry.getName();
            offset +=
                    OFFSET_TO_FILE_NAME
                            + name.length()
                            + entry.getCompressedSize()
                            + (entry.getExtra() == null ? 0 : entry.getExtra().length);
            if (entry.isDirectory()) {
                offset -= entry.getCompressedSize();
                continue;
            }
            if (PAYLOAD_BINARY_FILE_NAME.equals(name)) {
                if (entry.getMethod() != ZipEntry.STORED) {
                    Log.w(TAG, "Invalid compression method.");
                    return false;
                }
                mSize = entry.getCompressedSize();
                mOffset = offset - entry.getCompressedSize();
            } else if (PAYLOAD_PROPERTIES_FILE_NAME.equals(name)) {
                try (BufferedReader bufferedReader =
                        new BufferedReader(
                                new InputStreamReader(mUpdateZipFile.getInputStream(entry)))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        mProperties.add(line);
                    }
                }
            }
        }
        return true;
    }

    private UpdateEngine getUpdateEngine() {
        UpdateEngine updateEngine = new UpdateEngine();
        updateEngine.bind(new OtaUpdateCallback(updateEngine));
        return updateEngine;
    }

    private void applyPayload(String updatePath) throws IOException {
        if (!updatePayloadMetadata()) {
            return;
        }
        String[] header = mProperties.stream().toArray(String[]::new);
        if (mSize == -1) {
            Log.e(TAG, "No payload in package file.");
            return;
        }
        try {
            UpdateEngine updateEngine = getUpdateEngine();
            updateEngine.applyPayload(updatePath, mOffset, mSize, header);
        } catch (Exception e) {
            Log.e(TAG, "Failed to install update.", e);
        }
    }

    void applyUpdate(boolean optionEnabled) {
        String updateFilePath = optionEnabled ? OTA_16k_PATH : OTA_4k_PATH;
        try {
            mUpdateFilePath = new File(updateFilePath);
            clearPayloadMetadata();
            applyPayload(Paths.get(mUpdateFilePath.getAbsolutePath()).toUri().toString());

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

    void displayToast(String message) {
        ThreadUtils.postOnMainThread(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT));
    }

    class OtaUpdateCallback extends UpdateEngineCallback {
        UpdateEngine mUpdateEngine;

        OtaUpdateCallback(UpdateEngine engine) {
            mUpdateEngine = engine;
        }

        @Override
        public void onStatusUpdate(int status, float percent) {}

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            mUpdateEngine.unbind();
            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                Log.i(TAG, "applyPayload successful");
                PowerManager pm = mContext.getSystemService(PowerManager.class);
                pm.reboot(null);
            } else {
                Log.e(TAG, "apply payload failed, errocode: " + errorCode);
                displayToast("Update failed");
            }
        }
    }
}

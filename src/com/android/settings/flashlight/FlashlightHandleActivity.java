/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.flashlight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless activity that toggles flashlight state when launched.
 */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class FlashlightHandleActivity extends Activity implements Indexable {

    public static final String EXTRA_FALLBACK_TO_HOMEPAGE = "fallback_to_homepage";

    private static final String TAG = "FlashlightActivity";
    private static final String DATA_KEY = "flashlight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Control the flashlight
        controlFlashlight();

        // Caller's choice: fallback to homepage, or just exit?
        if (getIntent().getBooleanExtra(EXTRA_FALLBACK_TO_HOMEPAGE, false)) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
        finish();
    }

    private void controlFlashlight() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = getCameraId(this);
            if (cameraId != null) {
                boolean isFlashlightOn = isFlashlightEnabled(this);
                cameraManager.setTorchMode(cameraId, !isFlashlightOn);
                Settings.Secure.putInt(getContentResolver(), Settings.Secure.FLASHLIGHT_ENABLED, !isFlashlightOn ? 1 : 0);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to toggle flashlight: " + e.getMessage());
        }
    }

    private static String getCameraId(Context context) throws CameraAccessException {
        CameraManager cameraManager = context.getSystemService(CameraManager.class);
        String[] ids = cameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private static boolean isFlashlightEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.FLASHLIGHT_ENABLED, 0) == 1;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<>();
                    if (!context.getResources().getBoolean(
                            R.bool.config_settingsintelligence_slice_supported)) {
                        Log.d(TAG, "Search doesn't support Slice");
                        return result;
                    }

                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.power_flashlight);
                    data.screenTitle = context.getString(R.string.power_flashlight);
                    data.keywords = context.getString(R.string.keywords_flashlight);
                    data.intentTargetPackage = context.getPackageName();
                    data.intentTargetClass = FlashlightHandleActivity.class.getName();
                    data.intentAction = Intent.ACTION_MAIN;
                    data.key = DATA_KEY;
                    result.add(data);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    if (!FlashlightSlice.isFlashlightAvailable(context)) {
                        Log.i(TAG, "Flashlight is unavailable");
                        keys.add(DATA_KEY);
                    }
                    return keys;
                }
            };
}

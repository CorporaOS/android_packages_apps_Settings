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

package com.android.settings.deviceinfo.storage;

import static android.content.pm.PackageInfo.INSTALL_LOCATION_AUTO;
import static android.content.pm.PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.StorageWizardMoveAllConfirm;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveAllAppsPreferenceController extends BasePreferenceController {

    private static final String TAG = "MoveAllAppsPreferenceController";
    private final Context mContext;
    private final PackageManager mPm;
    private VolumeInfo mCurrentVolInfo;
    private Preference mMoveAllAppsPreference;
    private boolean mIsVolumeInfoUpdated;
    private ArrayList<String> mFilteredPackageNames = new ArrayList<>();

    public MoveAllAppsPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mPm = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mMoveAllAppsPreference = screen.findPreference(getPreferenceKey());
        mMoveAllAppsPreference.setVisible(false);
        mMoveAllAppsPreference.setEnabled(false);
        mMoveAllAppsPreference.setIcon(R.drawable.ic_swap_horiz);
    }

    @Override
    public void updateState(Preference preference) {
        if (!mIsVolumeInfoUpdated) {
            // Returns here to avoid jank by unnecessary UI update.
            return;
        }
        mIsVolumeInfoUpdated = false;
        if (mCurrentVolInfo != null) {
            mMoveAllAppsPreference.setSummary(getSummaryInternal());
            mMoveAllAppsPreference.setVisible(true);
        } else {
            mMoveAllAppsPreference.setVisible(false);
        }
        mMoveAllAppsPreference.setEnabled(!mFilteredPackageNames.isEmpty());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        final Intent intent = new Intent(mContext, StorageWizardMoveAllConfirm.class);
        intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mCurrentVolInfo.getId());
        intent.putExtra(Intent.EXTRA_PACKAGES, mFilteredPackageNames.toArray(new String[0]));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

    /**
     *
     * Used to set the current volume on which the app exists
     * @param volInfo an object representing the volume
     *
     */
    public void setCurrentVolume(VolumeInfo volInfo) {
        mCurrentVolInfo = volInfo;
        updateAppListAndUi();
    }

    private void updateAppListAndUi() {
        final List<PackageInfo> installedPackages = mPm.getInstalledPackages(0 /* flag */);
        mFilteredPackageNames.clear();
        for (PackageInfo pkg: installedPackages) {
            if (shouldFilterApplication(pkg)) continue;
            mFilteredPackageNames.add(pkg.packageName);
        }
        if (mMoveAllAppsPreference != null) {
            mIsVolumeInfoUpdated = true;
            updateState(mMoveAllAppsPreference);
        }
    }

    private boolean shouldFilterApplication(PackageInfo pkg) {
        if (pkg == null) {
            return true;
        }
        final int installLocationFlag = pkg.installLocation;
        if (installLocationFlag != INSTALL_LOCATION_AUTO
                && installLocationFlag != INSTALL_LOCATION_PREFER_EXTERNAL) {
            return true;
        }
        if (pkg.applicationInfo == null || pkg.applicationInfo.isSystemApp()) {
            return true;
        }
        final VolumeInfo installedVol = mPm.getPackageCurrentVolume(pkg.applicationInfo);
        return Objects.equals(installedVol, mCurrentVolInfo);
    }

    private CharSequence getSummaryInternal() {
        return StringUtil.getIcuPluralsString(mContext, mFilteredPackageNames.size(),
                R.string.move_all_apps_count);
    }
}

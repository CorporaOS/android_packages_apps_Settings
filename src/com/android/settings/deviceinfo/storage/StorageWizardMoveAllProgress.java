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

package com.android.settings.deviceinfo;

import static android.content.Intent.EXTRA_PACKAGES;
import static android.content.Intent.EXTRA_TITLE;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Arrays;
import java.util.Iterator;

public class StorageWizardMoveAllProgress extends StorageWizardBase {
    private static final String TAG = "StorageWizardMoveAllProgress";

    private Iterator<String> mPackageNamesIterator;
    private int mFailedToMoveCount;
    private int mMoveId;
    private int mPerAppPercent;
    private int mCurrentProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] packageNames = getIntent().getStringArrayExtra(EXTRA_PACKAGES);
        if (mVolume == null || packageNames == null || packageNames.length == 0) {
            finish();
            return;
        }

        setContentView(R.layout.storage_wizard_progress);

        final String appCount = getIntent().getStringExtra(EXTRA_TITLE);
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);
        mPerAppPercent = 100 / packageNames.length;
        mCurrentProgress = 0;
        mFailedToMoveCount = 0;
        mPackageNamesIterator = Arrays.asList(packageNames).iterator();

        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_move_all_apps_title, appCount);
        setBodyText(R.string.storage_wizard_move_all_apps_body, volumeName);
        setBackButtonVisibility(View.INVISIBLE);
        setNextButtonVisibility(View.INVISIBLE);
        // Register for updates and push through current status
        getPackageManager().registerMoveCallback(mCallback, new Handler());
        ThreadUtils.postOnBackgroundThread(() -> moveNextApp());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPackageManager().unregisterMoveCallback(mCallback);
    }

    private void moveNextApp() {
        if (mPackageNamesIterator.hasNext()) {
            mMoveId = getPackageManager().movePackage(mPackageNamesIterator.next(), mVolume);
            mCallback.onStatusChanged(mMoveId, getPackageManager().getMoveStatus(mMoveId), -1);
            return;
        }
        ThreadUtils.postOnMainThread(() -> {
            showToastIfAppsFailedToMove();
            finishAffinity();
        });
    }

    private void setCurrentProgressInternal(int progress) {
        ThreadUtils.postOnMainThread(() -> setCurrentProgress(progress));
    }

    private void showToastIfAppsFailedToMove() {
        if (mFailedToMoveCount > 0) {
            final String failedMoveCount = StringUtil.getIcuPluralsString(this, mFailedToMoveCount,
                    R.string.storage_wizard_move_all_apps_failed_to_move_count);
            Toast.makeText(StorageWizardMoveAllProgress.this, failedMoveCount, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private final MoveCallback mCallback = new MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (mMoveId != moveId) return;

            if (PackageManager.isMoveStatusFinished(status)) {
                Log.d(TAG, "Finished with status " + status);
                if (status != PackageManager.MOVE_SUCCEEDED) {
                    mFailedToMoveCount += 1;
                }
                mCurrentProgress += mPerAppPercent;
                setCurrentProgressInternal(mCurrentProgress);
                ThreadUtils.postOnBackgroundThread(() -> moveNextApp());
            } else {
                final int incrementalProgress = mCurrentProgress + (mPerAppPercent * status) / 100;
                setCurrentProgressInternal(incrementalProgress);
            }
        }
    };
}

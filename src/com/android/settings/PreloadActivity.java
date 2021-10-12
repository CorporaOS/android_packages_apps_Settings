/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

/**
 * Activity to support PreloadPreferenceFragment.
 *
 * Within the design of SettingsActivity, there're multiple steps before
 * starting the operating of PreferenceFragment. Initialization of
 * Activity/layout/theme would be run prior to the start of Fragment.
 * Which is to say, PreferenceFragment#onCreate() would be run after all of these.
 *
 * This activity will invoke #onFragmentPreload(Context) once it constructed.
 * This gives PreferenceFragment a chance to start some background works in
 * the very first beginning of the time in parallel while main thread
 * is keeping initialization. Which enables a possibility to reduce the time
 * required for first launch.
 *
 * Note: Since PreferenceFragment has not been management by FragmentManager,
 *       all of the APIs provided by PreferenceFragment are not functional.
 */
public class PreloadActivity extends SettingsActivity {
    private String mFragmentName;
    private Bundle mFragmentArgs;
    private Fragment mFragment;
    private boolean mEndOfPreload;

    @Override
    public String getInitialFragmentName(Intent intent) {
        if (mEndOfPreload) {
            return super.getInitialFragmentName(intent);
        }
        mFragmentName = super.getInitialFragmentName(intent);
        mFragmentArgs = super.getInitialFragmentArgs(intent);
        mFragment = constructFragment(mFragmentName, mFragmentArgs);
        if ( (mFragment != null) &&
                (mFragment instanceof PreloadPreferenceFragment) ) {
            ((PreloadPreferenceFragment)mFragment).onFragmentPreload(this);
        }
        return mFragmentName;
    }

    @Override
    Fragment constructFragment(String fragmentName, Bundle args) {
        if ( (mFragment != null) && TextUtils.equals(mFragmentName, fragmentName) ) {
            Fragment fragment = mFragment;
            mFragment = null;
            mEndOfPreload = true;
            return fragment;
        }
        return super.constructFragment(fragmentName, args);
    }
}

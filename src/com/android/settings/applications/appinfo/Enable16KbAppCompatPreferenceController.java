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

package com.android.settings.applications.appinfo;

import android.content.Context;

import androidx.preference.PreferenceScreen;
import androidx.preference.Preference;
import com.android.settings.SettingsPreferenceFragment;

public class Enable16KbAppCompatPreferenceController extends AppInfoPreferenceControllerBase {

    public Enable16KbAppCompatPreferenceController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return Enable16KbAppCompatDetails.class;
    }

    @Override
    public CharSequence getSummary() {
        return Enable16KbAppCompatDetails.getSummary(mContext, mParent.getAppEntry());
    }
}

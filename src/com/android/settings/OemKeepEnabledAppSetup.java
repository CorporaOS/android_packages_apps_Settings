/**
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

package com.android.settings;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Set;

/**
 * Listens to {@link Intent.ACTION_PRE_BOOT_COMPLETED}. 'Keep enabled' apps should not be in the
 * disabled state after SW upgrade.
 */
public class OemKeepEnabledAppSetup extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent broadcast) {
        // Change 'keep enabled' package setting to its default state.
        // Note: This happens if the disabled system app become 'keep enabled' after OTA or SW
        // upgrade.
        setEnabledSettingForKeepEnabledPackages(context,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP,
                goAsync());
    }

    /**
     * Set the enabled setting for a keep-enabled apps.
     *
     * @param context The context.
     * @param newState The new enabled state for the component. The legal values for
     *      this state are: {@link #COMPONENT_ENABLED_STATE_ENABLED},
     *      {@link #COMPONENT_ENABLED_STATE_DISABLED} or {@link #COMPONENT_ENABLED_STATE_DEFAULT}.
     *      The last one removes the setting, thereby restoring the component's state to whatever
     *      was set in its manifest (or enabled, by default).
     * @param asyncResult The async PendingResult
     * @param flags Optional behavior flags: {@link #DONT_KILL_APP} or 0.
     */
    private static void setEnabledSettingForKeepEnabledPackages(@NonNull Context context,
            int newState, int flags, final PendingResult asyncResult) {
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                final FeatureFactory factory = FeatureFactory.getFactory(context);
                final ApplicationFeatureProvider afp =
                        factory.getApplicationFeatureProvider(context);
                final Set<String> oemKeepEnabledPackages = afp.getOemKeepEnabledPackages();
                final PackageManager pm = context.getPackageManager();
                for (String packageName : oemKeepEnabledPackages) {
                    try {
                        int currentState = pm.getApplicationEnabledSetting(packageName);
                        if (currentState != newState &&
                                currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                            pm.setApplicationEnabledSetting(packageName, newState, flags);
                        }
                    } catch (IllegalArgumentException e) {
                        // Package does not exist
                        continue;
                    }
                }
                asyncResult.finish();
            }
        });
    }
}

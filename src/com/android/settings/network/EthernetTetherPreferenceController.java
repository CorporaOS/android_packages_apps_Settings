/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.TetheringManager;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;

import java.util.HashMap;

/**
 * This controller helps to manage the switch state and visibility of ethernet tether switch
 * preference.
 */
public final class EthernetTetherPreferenceController extends TetherBasePreferenceController {

    private final Context mContext;
    private final HashMap<String, IpConfiguration> mEthernetAvailableInterfaces = new HashMap<>();
    private final EthernetManager mEthernetManager;
    @VisibleForTesting
    EthernetManager.Listener mEthernetListener;

    public EthernetTetherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mEthernetManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mEthernetListener = (iface, linkStatus, role, configuration) -> {
            if (linkStatus == EthernetManager.LINK_UP) {
                mEthernetAvailableInterfaces.putIfAbsent(iface, configuration);
            } else {
                mEthernetAvailableInterfaces.remove(iface, configuration);
            }
            updateState(mPreference);
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        // Executor will execute to post the updateState event to a new handler which is created
        // from the main looper when the {@link EthernetManager.Listener.onAvailabilityChanged}
        // is triggerd.
        mEthernetManager.addListener(r -> handler.post(r), mEthernetListener);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mEthernetManager.removeListener(mEthernetListener);
        mEthernetListener = null;
    }

    @Override
    public boolean shouldEnable() {
        String[] available = mTm.getTetherableIfaces();
        for (String s : available) {
            if (mEthernetAvailableInterfaces.containsKey(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldShow() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_ETHERNET);
    }

    @Override
    public int getTetherType() {
        return TetheringManager.TETHERING_ETHERNET;
    }
}

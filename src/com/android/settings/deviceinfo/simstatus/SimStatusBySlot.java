/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.deviceinfo.simstatus;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

/**
 * A class for retrieving subscription informations on slot basis.
 */
public class SimStatusBySlot {

    private static final String TAG = "SimStatusBySlot";

    private final Phaser mBlocker = new Phaser(1);
    private final AtomicInteger mNumberOfSlots = new AtomicInteger(0);
    private ExecutorService mExecutor;
    private final ConcurrentHashMap<Integer, SubscriptionInfo> mSubscriptionMap =
            new ConcurrentHashMap<Integer, SubscriptionInfo>();

    private static final String KEY_SIM_STATUS = SimStatusPreferenceController.KEY_SIM_STATUS;
    private static final String KEY_SIM_STATUS_ABSENT = KEY_SIM_STATUS + "-";

    /**
     * Construct of class.
     * @param context Context
     */
    public static SimStatusBySlot get(Context context) {
        return new SimStatusBySlot(context, null);
    }

    /**
     * Construct of class in async manner.
     * @param context Context
     */
    public static SimStatusBySlot asyncGet(Context context) {
        return new SimStatusBySlot(context, Executors.newSingleThreadExecutor());
    }

    @VisibleForTesting
    protected SimStatusBySlot(Context context, ExecutorService executor) {
        if (context == null) {
            mBlocker.arrive();
        } else if (executor == null) {
            queryRecords(context);
        } else {
            mExecutor = executor;
            executor.execute(() -> queryRecords(context));
        }
    }

    protected void queryRecords(Context context) {
        if (isQueryPermitted(context)) {
            prepareEntries(context);
        }
        mBlocker.arrive();
    }

    protected boolean isQueryPermitted(Context context) {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) {
            Log.d(TAG, "abandened due to not support hardware.");
            return false;
        }
        return true;
    }

    protected void prepareEntries(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        if (telMgr == null) {
            return;
        }
        mNumberOfSlots.set(telMgr.getActiveModemCount());

        SubscriptionManager subMgr = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subMgr == null) {
            return;
        }

        List<SubscriptionInfo> subInfoList = subMgr.getActiveSubscriptionInfoList();
        if ((subInfoList == null) || (subInfoList.size() <= 0)) {
            Log.d(TAG, "no active SIM.");
            return;
        }

        final AtomicInteger maxSlotIndex = new AtomicInteger(-1);
        subInfoList.forEach(subInfo -> {
            int slotIndex = subInfo.getSimSlotIndex();
            mSubscriptionMap.put(slotIndex, subInfo);
            maxSlotIndex.set(Math.max(slotIndex, maxSlotIndex.get()));
        });
        Log.d(TAG, "active SIM: " + subInfoList.size());
    }

    protected void waitForResult() {
        mBlocker.awaitAdvance(0);
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

    /**
     * Number of slots available.
     * @return number of slots
     */
    public int size() {
        waitForResult();
        return mNumberOfSlots.get();
    }

    /**
     * Get subscription based on slot index.
     * @param slotIndex index of slot (starting from 0)
     * @return SubscriptionInfo based on index of slot.
     *         {@code null} means no subscription on slot.
     */
    public SubscriptionInfo getSubscriptionInfo(int slotIndex) {
        if (slotIndex >= size()) {
            return null;
        }
        return mSubscriptionMap.get(slotIndex);
    }

    /**
     * Generate key for Preference based on slot index.
     * @param slotIndex index of slot (starting from 0)
     * @return Preference key.
     */
    public String getPreferenceKey(int slotIndex) {
        SubscriptionInfo subInfo = getSubscriptionInfo(slotIndex);
        if (subInfo == null) {
            if (slotIndex < 0) {
                return KEY_SIM_STATUS;
            }
            return KEY_SIM_STATUS_ABSENT + slotIndex;
        }
        return KEY_SIM_STATUS + subInfo.getSubscriptionId();
    }
}

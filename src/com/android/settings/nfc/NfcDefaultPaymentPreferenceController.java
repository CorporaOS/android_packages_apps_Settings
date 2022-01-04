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

package com.android.settings.nfc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * NfcDefaultPaymentPreferenceController shows an app icon and text summary for current selected
 * default payment, and links to the nfc default payment selection page.
 */
public class NfcDefaultPaymentPreferenceController extends BasePreferenceController implements
        PaymentBackend.Callback, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "NfcDefaultPaymentController";

    private PaymentBackend mPaymentBackend;
    private Preference mPreference;

    public NfcDefaultPaymentPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setPaymentBackend(PaymentBackend backend) {
        mPaymentBackend = backend;
    }

    @Override
    public void onStart() {
        if (mPaymentBackend != null) {
            mPaymentBackend.registerCallback(this);
        }
    }

    @Override
    public void onStop() {
        if (mPaymentBackend != null) {
            mPaymentBackend.unregisterCallback(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (NfcAdapter.getDefaultAdapter(mContext) == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mPaymentBackend == null) {
            mPaymentBackend = new PaymentBackend(mContext);
        }
        final List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        return (appInfos != null && !appInfos.isEmpty())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
        if (defaultApp != null) {
            UserManager um = mContext.createContextAsUser(
                    defaultApp.userHandle, /*flags=*/0).getSystemService(UserManager.class);
            boolean isManagedProfile = um.isManagedProfile(defaultApp.userHandle.getIdentifier());
            if (isManagedProfile) {
                preference.setSummary(defaultApp.label + " - Work");
            } else {
                preference.setSummary(defaultApp.label);
            }
            preference.setIcon(getFixedSizeDrawable(defaultApp.icon, 200, 200));
        } else {
            preference.setSummary(mContext.getText(R.string.nfc_payment_default_not_set));
        }
        preference.setIconSpaceReserved(true);
    }

    @Override
    public CharSequence getSummary() {
        final PaymentAppInfo defaultApp = mPaymentBackend.getDefaultApp();
        if (defaultApp != null) {
            UserManager um = mContext.createContextAsUser(
                    defaultApp.userHandle, /*flags=*/0).getSystemService(UserManager.class);
            boolean isManagedProfile = um.isManagedProfile(defaultApp.userHandle.getIdentifier());
            if (isManagedProfile) {
                return defaultApp.label + " - Work";
            } else {
                return defaultApp.label;
            }
        } else {
            return mContext.getText(R.string.nfc_payment_default_not_set);
        }
    }

    @Override
    public void onPaymentAppsChanged() {
        updateState(mPreference);
    }

    private static Drawable getFixedSizeDrawable(Drawable original, int expectWidth,
            int expectHeight) {
        final int actualWidth = original.getIntrinsicWidth();
        final int actualHeight = original.getIntrinsicHeight();

        final float scaleWidth = ((float) expectWidth) / actualWidth;
        final float scaleHeight = ((float) expectHeight) / actualHeight;
        final float scale = Math.min(scaleWidth, scaleHeight);
        final int width = (int) (actualWidth * scale);
        final int height = (int) (actualHeight * scale);

        return getBitmapDrawable(original, width, height);
    }

    private static Drawable getBitmapDrawable(Drawable original, int width, int height) {
        final Bitmap bitmap;

        if (original instanceof BitmapDrawable) {
            bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) original).getBitmap(), width,
                    height, false);
        } else {
            bitmap = Utils.createBitmap(original, width, height);
        }
        return new BitmapDrawable(null, bitmap);
    }

}

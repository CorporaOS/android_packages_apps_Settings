/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.vpn2;

import android.content.Context;
import android.net.VpnManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.security.Credentials;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;

/**
 * Utility functions for vpn.
 */
public class VpnUtils {

    private static final String TAG = "VpnUtils";

    /** Get the lockdown vpn key stored in the database, set by setLockdownVpn */
    public static @Nullable String getLockdownVpn(@NonNull Context context) {
        final byte[] value = getVpnManager(context).getVpnProfile(Credentials.LOCKDOWN_VPN);
        return value == null ? null : new String(value);
    }

    /** Clear the lockdown vpn key. */
    public static void clearLockdownVpn(@NonNull Context context) {
        getVpnManager(context).removeVpnProfile(Credentials.LOCKDOWN_VPN);
        // Always notify VpnManager after keystore update
        getVpnManager(context).updateLockdownVpn();
    }

    /** Set the lockdown vpn key. */
    public static void setLockdownVpn(@NonNull Context context, @NonNull String lockdownKey) {
        getVpnManager(context).putVpnProfile(Credentials.LOCKDOWN_VPN, lockdownKey.getBytes());
        // Always notify VpnManager after keystore update
        getVpnManager(context).updateLockdownVpn();
    }

    /**
     * Returns whether the given key is the currently set lockdown vpn key, set by setLockdownVpn.
     */
    public static boolean isVpnLockdown(@NonNull Context context, @NonNull String key) {
        return key.equals(getLockdownVpn(context));
    }

    public static boolean isAnyLockdownActive(Context context) {
        final int userId = context.getUserId();
        if (getLockdownVpn(context) != null) {
            return true;
        }
        return getVpnManager(context).getAlwaysOnVpnPackageForUser(userId) != null
                && Settings.Secure.getIntForUser(context.getContentResolver(),
                        Settings.Secure.ALWAYS_ON_VPN_LOCKDOWN, /* default */ 0, userId) != 0;
    }

    public static boolean isVpnActive(Context context) throws RemoteException {
        return getVpnManager(context).getVpnConfig(context.getUserId()) != null;
    }

    public static String getConnectedPackage(VpnManager vpnManager, final int userId) {
        final VpnConfig config = vpnManager.getVpnConfig(userId);
        return config != null ? config.user : null;
    }

    private static VpnManager getVpnManager(Context context) {
        return context.getSystemService(VpnManager.class);
    }

    public static boolean isAlwaysOnVpnSet(VpnManager vm, final int userId) {
        return vm.getAlwaysOnVpnPackageForUser(userId) != null;
    }

    public static boolean disconnectLegacyVpn(Context context) {
        int userId = context.getUserId();
        LegacyVpnInfo currentLegacyVpn = getVpnManager(context).getLegacyVpnInfo(userId);
        if (currentLegacyVpn != null) {
            clearLockdownVpn(context);
            getVpnManager(context).prepareVpn(null, VpnConfig.LEGACY_VPN, userId);
            return true;
        }
        return false;
    }
}

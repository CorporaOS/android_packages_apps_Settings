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
package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.CarrierConfigCache;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public final class Enable2gPreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final String PREFERENCE_KEY = "TEST_2G_PREFERENCE";

    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;

    private RestrictedSwitchPreference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private PersistableBundle mPersistableBundle;
    private Enable2gPreferenceController mController;
    private Context mContext;

    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private AllowedNetworkTypesListener mMockAllowedNetworkTypesListener;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mController = new Enable2gPreferenceController(mContext, PREFERENCE_KEY);

        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);

        mPreference = spy(new RestrictedSwitchPreference(mContext));
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
        mController.init(SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnavailable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nullCarrierConfig_returnUnavailable() {
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        doReturn(null).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_capabilityNotSupported_returnUnavailable() {
        doReturn(false).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedTrue_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedFalse_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void setChecked_disabledByAdmin_returnFalse() {
        when2gIsDisabledByAdmin(true);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void onPreferenceChange_updateDisable() {
        when2gIsEnabledForReasonEnable2g();

        // Disable 2G
        boolean changed = mController.setChecked(false);
        assertThat(changed).isEqualTo(true);

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                TelephonyManager.NETWORK_TYPE_BITMASK_LTE);
    }

    @Test
    public void onPreferenceChange_updateEnable() {
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_LTE));

        // Enable 2G
        boolean changed = mController.setChecked(true);
        assertThat(changed).isEqualTo(true);

        long allowedNetworkTypes = TelephonyManager.NETWORK_TYPE_BITMASK_LTE
                | TelephonyManager.NETWORK_CLASS_BITMASK_2G;
        // Set the allowed network types.
        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                allowedNetworkTypes);
    }

    @Test
    public void disabledByAdmin_toggleUnchecked() {
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void userRestrictionInactivated_userToggleMaintainsState() {
        // Initially, 2g is enabled
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isTrue();

        // When we disable the preference by an admin, the preference should be unchecked
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isFalse();

        // If the preference is re-enabled by an admin, former state should hold
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void whenChangeNetworkTypeTo2gOnly_toggleBehavior() {
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn((long) 0);
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) TelephonyManager.NETWORK_TYPE_BITMASK_GSM);
        when2gIsDisabledByAdmin(false);
        long networkType = (long) RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_GSM_ONLY);

        // Set 2G only to the preferred network type.
        mController.mAllowedNetworkTypesListener.onAllowedNetworkTypesChanged(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, networkType);

        // Set the allowed network types for reason enable 2G.
        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                TelephonyManager.NETWORK_CLASS_BITMASK_2G);
        // Allow 2G is grayed out
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void whenChangeNetworkTypeToNot2gOnly_toggleBehavior() {
        when2gIsEnabledForReasonEnable2g();
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                        | TelephonyManager.NETWORK_TYPE_BITMASK_LTE));
        when2gIsDisabledByAdmin(false);
        long networkType = (long) RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_LTE_ONLY);

        // Set not 2G only to the preferred network type.
        mController.mAllowedNetworkTypesListener.onAllowedNetworkTypesChanged(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, networkType);

        // Allow 2G is de-grayed out
        verify(mPreference).setEnabled(true);
    }

    @Test
    @UiThreadTest
    public void goThroughLifecycle_shouldRegisterUnregisterNetworkTypesListener() {
        Enable2gPreferenceController controller =
                spy(new Enable2gPreferenceController(mContext, PREFERENCE_KEY));
        mLifecycleRegistry.addObserver(controller);
        when(controller.isAvailable()).thenReturn(true);
        controller.mAllowedNetworkTypesListener = mMockAllowedNetworkTypesListener;

        mLifecycleRegistry.handleLifecycleEvent(ON_START);
        verify(controller).onStart();
        verify(controller.mAllowedNetworkTypesListener).register(any(), anyInt());

        mLifecycleRegistry.handleLifecycleEvent(ON_STOP);
        verify(controller).onStop();
        verify(controller.mAllowedNetworkTypesListener).unregister(any(), anyInt());
    }

    private void when2gIsEnabledForReasonEnable2g() {
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                        | TelephonyManager.NETWORK_TYPE_BITMASK_LTE));
    }

    private void when2gIsDisabledByAdmin(boolean is2gDisabledByAdmin) {
        // Our controller depends on state being initialized when the associated preference is
        // displayed because the admin disablement functionality flows from the association of a
        // Preference with the PreferenceScreen
        when(mPreference.isDisabledByAdmin()).thenReturn(is2gDisabledByAdmin);
        mController.displayPreference(mPreferenceScreen);
    }
}

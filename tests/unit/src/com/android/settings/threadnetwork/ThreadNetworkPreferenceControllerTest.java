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

package com.android.settings.threadnetwork;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.thread.ThreadNetworkController.StateCallback;
import android.net.thread.ThreadNetworkException;
import android.os.OutcomeReceiver;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.threadnetwork.ThreadNetworkPreferenceController.BaseThreadNetworkController;

import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link ThreadNetworkPreferenceController}. */
@RunWith(AndroidJUnit4.class)
public final class ThreadNetworkPreferenceControllerTest {
    private static final String TEST_SUMMARY = "Thread is great";
    private static final String TEST_AIRPLANE_SUMMARY = "Turn off airplane mode to use Thread";

    private Context mContext;
    private Executor mExecutor;
    private ThreadNetworkPreferenceController mController;
    private FakeThreadNetworkController mFakeThreadNetworkController;

    private SwitchPreference mPreference;

    private ArgumentCaptor<BroadcastReceiver> mBroadcastReceiverArgumentCaptor =
            ArgumentCaptor.forClass(BroadcastReceiver.class);

    @Before
    public void setUp() throws Exception {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mExecutor = ContextCompat.getMainExecutor(mContext);
        mFakeThreadNetworkController =
                new FakeThreadNetworkController(mExecutor);
        mController = newControllerWithThreadFeatureSupported(true);

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey("thread_network_settings");
        preferenceScreen.addPreference(mPreference);
        mController.displayPreference(preferenceScreen);
    }

    private ThreadNetworkPreferenceController newControllerWithThreadFeatureSupported(
            boolean present) {
        return new ThreadNetworkPreferenceController(
                mContext,
                "thread_network_settings" /* key */,
                mExecutor,
                present ? mFakeThreadNetworkController : null);
    }

    @Test
    public void getAvailabilityStatus_airPlaneModeOn_returnsDisabledDependentSetting()
            throws Exception {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        mController.onStart();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_airPlaneModeOff_returnsAvailable() throws Exception {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        mController.onStart();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_threadFeatureNotSupported_returnsUnsupported() {
        mController = newControllerWithThreadFeatureSupported(false);

        mController.onStart();

        assertThat(mFakeThreadNetworkController.getRegisteredStateCallback()).isNull();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_threadSetEnabled_returnsTrue() {
        mFakeThreadNetworkController.setThreadEnabled(true, mExecutor, unused -> {});

        mController.onStart();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_threadSetDisabled_returnsFalse() {
        mFakeThreadNetworkController.setThreadEnabled(false, mExecutor, unused -> {});

        mController.onStart();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setChecked_threadIsEnabled() {
        mController.onStart();
        mController.setChecked(true);

        assertThat(mFakeThreadNetworkController.isEnabled()).isTrue();
    }

    @Test
    public void setChecked_setUnckecked_threadIsDisabled() {
        mController.onStart();
        mController.setChecked(false);

        assertThat(mFakeThreadNetworkController.isEnabled()).isFalse();
    }

    @Test
    public void updatePreference_airPlaneModeOff_preferenceEnabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        mController.onStart();

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getResources().getString(R.string.thread_network_settings_summary));
    }

    @Test
    public void updatePreference_airPlaneModeOn_preferenceEnabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        mController.onStart();

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getResources().getString(
                        R.string.thread_network_settings_summary_airplane_mode));
    }

    private void startControllerAndCaptureCallbacks() {
        mController.onStart();
        verify(mContext).registerReceiver(
                mBroadcastReceiverArgumentCaptor.capture(), any());
    }

    @Test
    public void updatePreference_airPlaneModeTurnedOn_preferenceDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        startControllerAndCaptureCallbacks();
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mBroadcastReceiverArgumentCaptor.getValue().onReceive(mContext, new Intent());

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getResources().getString(
                        R.string.thread_network_settings_summary_airplane_mode));
    }

    private static final class FakeThreadNetworkController implements BaseThreadNetworkController {
        private final Executor mExecutor;
        private boolean mIsEnabled = true;
        @Nullable
        private StateCallback mStateCallback;

        FakeThreadNetworkController(Executor executor) {
            mExecutor = executor;
        }

        public boolean isEnabled() {
            return mIsEnabled;
        }

        public StateCallback getRegisteredStateCallback() {
            return mStateCallback;
        }

        @Override
        public void setThreadEnabled(
                boolean enabled,
                Executor executor,
                OutcomeReceiver<Void, ThreadNetworkException> receiver) {
            mIsEnabled = enabled;
            if (mStateCallback != null) {
                mExecutor.execute(() -> mStateCallback.onThreadEnabledChanged(mIsEnabled));
            }
            executor.execute(() -> receiver.onResult(null));
        }

        @Override
        public void registerStateCallback(Executor executor, StateCallback callback) {
            if (callback == mStateCallback) {
                throw new IllegalArgumentException("callback is already registered");
            }
            mStateCallback = callback;
            mExecutor.execute(() -> mStateCallback.onThreadEnabledChanged(mIsEnabled));
        }

        @Override
        public void unregisterStateCallback(StateCallback callback) {
            if (mStateCallback == null) {
                throw new IllegalArgumentException("callback is already unregistered");
            }
            mStateCallback = null;
        }
    }
}

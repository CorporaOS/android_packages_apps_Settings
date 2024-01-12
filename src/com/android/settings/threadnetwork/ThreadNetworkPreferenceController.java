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

import static android.net.thread.ThreadNetworkController.STATE_ENABLED;
import static android.provider.Settings.Global.AIRPLANE_MODE_ON;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.thread.ThreadNetworkController;
import android.net.thread.ThreadNetworkController.StateCallback;
import android.net.thread.ThreadNetworkException;
import android.net.thread.ThreadNetworkManager;
import android.os.OutcomeReceiver;
import android.provider.Settings.Global;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.net.thread.platform.flags.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.concurrent.Executor;

/** Controller for "Thread network" toggle. */
public class ThreadNetworkPreferenceController extends TogglePreferenceController implements
        LifecycleObserver {
    private static final String TAG = "ThreadNetworkSettings";
    private final Executor mExecutor;
    @Nullable
    private final BaseThreadNetworkController mThreadController;
    private final StateCallback mStateCallback;
    private final BroadcastReceiver mAirplaneModeReceiver;
    private boolean mEnabled;
    private boolean mAirplaneModeOn;
    @Nullable
    private Preference mPreference;

    /**
     * A testable interface for {@link ThreadNetworkController} which is `final`.
     *
     * We are in a awkward situation that Android API guideline suggest `final` for API classes
     * while Robolectric test is being deprecated for platform testing (See
     * tests/robotests/new_tests_hook.sh). This force us to use "mockito-target-extended" but it's
     * conflicting with the default "mockito-target" which is somehow iindirectly dependented by
     * the `SettingsUnitTests` target.
     */
    @VisibleForTesting
    interface BaseThreadNetworkController {
        void setEnabled(
                boolean enabled,
                Executor executor,
                OutcomeReceiver<Void, ThreadNetworkException> receiver);
        void registerStateCallback(Executor executor, StateCallback callback);
        void unregisterStateCallback(StateCallback callback);
    }

    public ThreadNetworkPreferenceController(Context context, String key) {
        this(
                context,
                key,
                ContextCompat.getMainExecutor(context),
                getThreadNetworkController(context));
    }

    @VisibleForTesting
    ThreadNetworkPreferenceController(
            Context context,
            String key,
            Executor executor,
            @Nullable BaseThreadNetworkController threadController) {
        super(context, key);
        mExecutor = executor;
        mThreadController = threadController;
        mStateCallback = newStateCallback();
        mAirplaneModeReceiver = newAirPlaneModeReceiver();
    }

    @Nullable
    private static final BaseThreadNetworkController getThreadNetworkController(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_THREAD_NETWORK)) {
            return null;
        }
        ThreadNetworkManager manager = context.getSystemService(ThreadNetworkManager.class);
        ThreadNetworkController controller = manager.getAllThreadNetworkControllers().get(0);
        return new BaseThreadNetworkController() {
            @Override
            public void setEnabled(
                    boolean enabled,
                    Executor executor,
                    OutcomeReceiver<Void, ThreadNetworkException> receiver) {
                controller.setEnabled(enabled, executor, receiver);
            }
            @Override
            public void registerStateCallback(Executor executor, StateCallback callback) {
                controller.registerStateCallback(executor, callback);
            }
            @Override
            public void unregisterStateCallback(StateCallback callback) {
                controller.unregisterStateCallback(callback);
            }
        };
    }

    public boolean isThreadSupportedOnDevice() {
        return mThreadController != null;
    }

    private StateCallback newStateCallback() {
        return new StateCallback() {
            @Override
            public void onThreadEnableStateChanged(int enabledState) {
                mEnabled = enabledState == STATE_ENABLED;
            }
            @Override
            public void onDeviceRoleChanged(int role) {}
        };
    }

    private BroadcastReceiver newAirPlaneModeReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAirplaneModeOn = isAirplaneModeOn(context);
                Log.i(TAG, "Airplane mode is " + (mAirplaneModeOn ? "ON" : "OFF"));
                if (mPreference != null) {
                    updateState(mPreference);
                }
            }
        };
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Global.getInt(context.getContentResolver(), AIRPLANE_MODE_ON, 0) == 1;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.threadEnabledPlatform()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (!isThreadSupportedOnDevice()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (mAirplaneModeOn) {
            return DISABLED_DEPENDENT_SETTING;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return !mAirplaneModeOn && mEnabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mThreadController == null) {
            return true;
        }
        final String action = (isChecked ? "enable" : "disable");
        mThreadController.setEnabled(
                isChecked,
                mExecutor,
                new OutcomeReceiver<Void, ThreadNetworkException>() {
                    @Override
                    public void onError(ThreadNetworkException e) {
                        Log.e(TAG, "Failed to " + action + " Thread", e);
                    }
                    @Override
                    public void onResult(Void unused) {
                        Log.d(TAG, "Successfully " + action + " Thread");
                    }
                });
        return true;
    }

    /** Called when activity starts being displayed to user. */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (mThreadController == null) {
            return;
        }
        mThreadController.registerStateCallback(mExecutor, mStateCallback);
        mAirplaneModeOn = isAirplaneModeOn(mContext);
        mContext.registerReceiver(
                mAirplaneModeReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    /** Called when activity stops being displayed to user. */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (mThreadController == null) {
            return;
        }
        mThreadController.unregisterStateCallback(mStateCallback);
        mContext.unregisterReceiver(mAirplaneModeReceiver);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mAirplaneModeOn);
        refreshSummary(mPreference);
    }

    @Override
    public CharSequence getSummary() {
        int resId;
        if (mAirplaneModeOn) {
            resId = R.string.thread_network_settings_summary_airplane_mode;
        } else {
            resId = R.string.thread_network_settings_summary;
        }
        return mContext.getResources().getString(resId);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_connected_devices;
    }
}


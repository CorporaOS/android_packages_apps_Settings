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
package com.android.settings.threadnetwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.net.thread.ThreadNetworkManager
import android.os.OutcomeReceiver
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.net.thread.platform.flags.Flags
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import java.util.concurrent.Executor

/** Controller for the "Thread" toggle in "Connected devices > Connection preferences".  */
class ThreadNetworkPreferenceController @VisibleForTesting constructor(
    context: Context,
    key: String,
    private val mExecutor: Executor,
    private val mThreadController: BaseThreadNetworkController?
) : TogglePreferenceController(context, key), LifecycleObserver {
    private val mStateCallback: StateCallback
    private val mAirplaneModeReceiver: BroadcastReceiver
    private var mEnabled = false
    private var mAirplaneModeOn = false
    private var mPreference: Preference? = null

    /**
     * A testable interface for [ThreadNetworkController] which is `final`.
     *
     * We are in a awkward situation that Android API guideline suggest `final` for API classes
     * while Robolectric test is being deprecated for platform testing (See
     * tests/robotests/new_tests_hook.sh). This force us to use "mockito-target-extended" but it's
     * conflicting with the default "mockito-target" which is somehow indirectly depended by the
     * `SettingsUnitTests` target.
     */
    @VisibleForTesting
    interface BaseThreadNetworkController {
        fun setEnabled(
            enabled: Boolean,
            executor: Executor,
            receiver: OutcomeReceiver<Void?, ThreadNetworkException>
        )

        fun registerStateCallback(executor: Executor, callback: StateCallback)

        fun unregisterStateCallback(callback: StateCallback)
    }

    constructor(context: Context, key: String) : this(
        context,
        key,
        ContextCompat.getMainExecutor(context),
        getThreadNetworkController(context)
    )

    init {
        mStateCallback = newStateCallback()
        mAirplaneModeReceiver = newAirPlaneModeReceiver()
    }

    val isThreadSupportedOnDevice: Boolean
        get() = mThreadController != null

    private fun newStateCallback(): StateCallback {
        return object : StateCallback {
            override fun onThreadEnableStateChanged(enabledState: Int) {
                mEnabled = enabledState == ThreadNetworkController.STATE_ENABLED
            }

            override fun onDeviceRoleChanged(role: Int) {}
        }
    }

    private fun newAirPlaneModeReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mAirplaneModeOn = isAirplaneModeOn(context)
                Log.i(TAG, "Airplane mode is " + if (mAirplaneModeOn) "ON" else "OFF")
                mPreference?.let { preference -> updateState(preference) }
            }
        }
    }

    override fun getAvailabilityStatus(): Int {
        if (!Flags.threadEnabledPlatform()) {
            return CONDITIONALLY_UNAVAILABLE
        }
        return if (!isThreadSupportedOnDevice) {
            UNSUPPORTED_ON_DEVICE
        } else if (mAirplaneModeOn) {
            DISABLED_DEPENDENT_SETTING
        } else {
            AVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
    }

    override fun isChecked(): Boolean {
        return !mAirplaneModeOn && mEnabled
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (mThreadController == null) {
            return false
        }
        val action = if (isChecked) "enable" else "disable"
        mThreadController.setEnabled(
            isChecked,
            mExecutor,
            object : OutcomeReceiver<Void?, ThreadNetworkException> {
                override fun onError(e: ThreadNetworkException) {
                    Log.e(TAG, "Failed to $action Thread", e)
                }

                override fun onResult(unused: Void?) {
                    Log.d(TAG, "Successfully $action Thread")
                }
            })
        return true
    }

    /** Called when activity starts being displayed to user.  */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (mThreadController == null) {
            return
        }
        mThreadController.registerStateCallback(mExecutor, mStateCallback)
        mAirplaneModeOn = isAirplaneModeOn(mContext)
        mContext.registerReceiver(
            mAirplaneModeReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )
        mPreference?.let { preference -> updateState(preference) }
    }

    /** Called when activity stops being displayed to user.  */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        if (mThreadController == null) {
            return
        }
        mThreadController.unregisterStateCallback(mStateCallback)
        mContext.unregisterReceiver(mAirplaneModeReceiver)
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isEnabled = !mAirplaneModeOn
        refreshSummary(mPreference)
    }

    override fun getSummary(): CharSequence {
        val resId: Int = if (mAirplaneModeOn) {
            R.string.thread_network_settings_summary_airplane_mode
        } else {
            R.string.thread_network_settings_summary
        }
        return mContext.getResources().getString(resId)
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_connected_devices
    }

    companion object {
        private const val TAG = "ThreadNetworkSettings"
        private fun getThreadNetworkController(context: Context): BaseThreadNetworkController? {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_THREAD_NETWORK)) {
                return null
            }
            val manager = context.getSystemService(ThreadNetworkManager::class.java) ?: return null
            val controller = manager.allThreadNetworkControllers[0]
            return object : BaseThreadNetworkController {
                override fun setEnabled(
                    enabled: Boolean,
                    executor: Executor,
                    receiver: OutcomeReceiver<Void?, ThreadNetworkException>
                ) {
                    controller.setEnabled(enabled, executor, receiver)
                }

                override fun registerStateCallback(executor: Executor, callback: StateCallback) {
                    controller.registerStateCallback(executor, callback)
                }

                override fun unregisterStateCallback(callback: StateCallback) {
                    controller.unregisterStateCallback(callback)
                }
            }
        }

        private fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        }
    }
}

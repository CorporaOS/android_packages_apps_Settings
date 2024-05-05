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
package com.android.settings.connecteddevice.threadnetwork

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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settings.flags.Flags
import com.android.settingslib.HelpUtils
import com.android.settingslib.widget.FooterPreference
import java.util.concurrent.Executor

/**
 * Controller for the "Use Thread" toggle in "Connected devices > Connection preferences > Thread".
 */
class ThreadNetworkPreferenceController @VisibleForTesting constructor(
    context: Context,
    key: String,
    private val executor: Executor,
    private val threadController: BaseThreadNetworkController?
) : TogglePreferenceController(context, key), LifecycleEventObserver {
    private val stateCallback: StateCallback
    private val airplaneModeReceiver: BroadcastReceiver
    private var threadEnabled = false
    private var airplaneModeOn = false
    private var preference: Preference? = null

    constructor(context: Context, key: String) : this(
        context,
        key,
        ContextCompat.getMainExecutor(context),
        Utils.getThreadNetworkController(context)
    )

    init {
        stateCallback = newStateCallback()
        airplaneModeReceiver = newAirPlaneModeReceiver()
    }

    val isThreadSupportedOnDevice: Boolean
        get() = threadController != null

    private fun newStateCallback(): StateCallback {
        return object : StateCallback {
            override fun onThreadEnableStateChanged(enabledState: Int) {
                threadEnabled = enabledState == ThreadNetworkController.STATE_ENABLED
                preference?.let { preference -> updateState(preference) }
            }

            override fun onDeviceRoleChanged(role: Int) {}
        }
    }

    private fun newAirPlaneModeReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                airplaneModeOn = isAirplaneModeOn(context)
                Log.i(TAG, "Airplane mode is " + if (airplaneModeOn) "ON" else "OFF")
                preference?.let { preference -> updateState(preference) }
            }
        }
    }

    override fun getAvailabilityStatus(): Int {
        return /*if (!Flags.threadSettingsEnabled()) {
            CONDITIONALLY_UNAVAILABLE
        } else */if (!isThreadSupportedOnDevice) {
            UNSUPPORTED_ON_DEVICE
        } else if (airplaneModeOn) {
            DISABLED_DEPENDENT_SETTING
        } else {
            AVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        val footer: FooterPreference? = screen.findPreference(KEY_PREFERENCE_FOOTER)
        if (footer != null) {
            setupFooterPreference(footer)
        }
    }

    private fun setupFooterPreference(footer: FooterPreference) {
        footer.setLearnMoreAction { v -> openLocaleLearnMoreLink() }
        footer.setLearnMoreText(mContext.getString(R.string.thread_network_settings_learn_more))
    }

    private fun openLocaleLearnMoreLink() {
        val intent = HelpUtils.getHelpIntent(
                mContext,
                mContext.getString(R.string.thread_network_settings_learn_more_link),
                mContext::class.java.name)
        if (intent != null) {
            mContext.startActivity(intent);
        } else {
            Log.w(TAG, "HelpIntent is null");
        }
    }

    override fun isChecked(): Boolean {
        // TODO (b/322742298):
        // Check airplane mode here because it's planned to disable Thread state in airplane mode
        // (code in the mainline module). But it's currently not implemented yet (b/322742298).
        // By design, the toggle should be unchecked in airplane mode, so explicitly check the
        // airplane mode here to archieve the same UX.
        return !airplaneModeOn && threadEnabled
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (threadController == null) {
            return false
        }

        // Avoids deadloop of setChecked -> threadController.setEnabled() ->
        // StateCallback.onThreadEnableStateChanged -> updateState -> setChecked
        if (isChecked == isChecked()) {
            return true
        }

        val action = if (isChecked) "enable" else "disable"
        threadController.setEnabled(
            isChecked,
            executor,
            object : OutcomeReceiver<Void?, ThreadNetworkException> {
                override fun onError(e: ThreadNetworkException) {
                    // TODO(b/327549838): gracefully handle the failure by resetting the UI state
                    Log.e(TAG, "Failed to $action Thread", e)
                }

                override fun onResult(unused: Void?) {
                    Log.d(TAG, "Successfully $action Thread")
                }
            })
        return true
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (threadController == null) {
            return
        }

        when (event) {
            Lifecycle.Event.ON_START -> {
                threadController.registerStateCallback(executor, stateCallback)
                airplaneModeOn = isAirplaneModeOn(mContext)
                mContext.registerReceiver(
                    airplaneModeReceiver,
                    IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                )
                preference?.let { preference -> updateState(preference) }
            }
            Lifecycle.Event.ON_STOP -> {
                threadController.unregisterStateCallback(stateCallback)
                mContext.unregisterReceiver(airplaneModeReceiver)
            }
            else -> {}
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isEnabled = !airplaneModeOn
        refreshSummary(preference)
    }

    override fun getSummary(): CharSequence {
        val resId: Int = if (airplaneModeOn) {
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
        private const val KEY_PREFERENCE_FOOTER = "thread_network_settings_footer"
        private fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        }
    }
}
